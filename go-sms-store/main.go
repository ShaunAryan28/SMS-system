package main

import (
	"context"
	"encoding/json"
	"log"
	"net/http"
	"strings"
	"time"

	"github.com/segmentio/kafka-go"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

// This struct defines what our data looks like in Go and MongoDB
type SmsRecord struct {
	UserID      string    `json:"userId" bson:"userId"`
	PhoneNumber string    `json:"phoneNumber" bson:"phoneNumber"`
	Message     string    `json:"message" bson:"message"`
	Status      string    `json:"status" bson:"status"`
	Timestamp   time.Time `json:"timestamp" bson:"timestamp"`
}

var collection *mongo.Collection

func main() {
	// 1. Initialize MongoDB Connection
	clientOptions := options.Client().ApplyURI("mongodb://localhost:27018")
	client, err := mongo.Connect(context.TODO(), clientOptions)
	if err != nil {
		log.Fatal("MongoDB connection error:", err)
	}
	collection = client.Database("smsdb").Collection("messages")
	log.Println("Connected to MongoDB!")

	// 2. Start Kafka Consumer in a background thread (Goroutine)
	go startKafkaConsumer()

	// 3. Setup standard net/http Server for the API
	mux := http.NewServeMux()
	mux.HandleFunc("/v1/user/", getUserMessagesHandler)

	log.Println("Go SMS Store running on port 8081...")
	log.Fatal(http.ListenAndServe(":8081", mux))
}

// Background task that constantly listens to Kafka
func startKafkaConsumer() {
	r := kafka.NewReader(kafka.ReaderConfig{
		Brokers:   []string{"localhost:9092"},
		Topic:     "sms-events",
		Partition: 0,
		MinBytes:  10e3, // 10KB
		MaxBytes:  10e6, // 10MB
	})

	log.Println("Listening for Kafka events on topic: sms-events...")

	for {
		m, err := r.ReadMessage(context.Background())
		if err != nil {
			log.Printf("Error reading kafka message: %v\n", err)
			continue
		}

		var record SmsRecord
		if err := json.Unmarshal(m.Value, &record); err != nil {
			log.Printf("Error decoding JSON: %v\n", err)
			continue
		}

		record.Timestamp = time.Now()

		// Save the caught event to MongoDB
		_, err = collection.InsertOne(context.TODO(), record)
		if err != nil {
			log.Printf("Error inserting to MongoDB: %v\n", err)
		} else {
			log.Printf("Successfully saved SMS to DB for user: %s with status: %s\n", record.UserID, record.Status)
		}
	}
}

// API Endpoint to fetch user history
func getUserMessagesHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// Extract {User_id} from URL: /v1/user/{User_id}/messages
	parts := strings.Split(r.URL.Path, "/")
	if len(parts) < 5 || parts[4] != "messages" {
		http.Error(w, "Invalid endpoint. Use /v1/user/{User_id}/messages", http.StatusBadRequest)
		return
	}
	userID := parts[3]

	// Find all messages for this user in MongoDB
	cursor, err := collection.Find(context.TODO(), bson.M{"userId": userID})
	if err != nil {
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}
	defer cursor.Close(context.TODO())

	var results []SmsRecord
	if err = cursor.All(context.TODO(), &results); err != nil {
		http.Error(w, "Error decoding results", http.StatusInternalServerError)
		return
	}

	// Send JSON response back to the client
	w.Header().Set("Content-Type", "application/json")
	if results == nil {
		results = []SmsRecord{} // Return empty array instead of null if no messages found
	}
	json.NewEncoder(w).Encode(results)
}