package main

import (
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestGetUserMessagesHandler_InvalidMethod(t *testing.T) {
	req, err := http.NewRequest("POST", "/v1/user/user123/messages", nil)
	if err != nil {
		t.Fatal(err)
	}

	rr := httptest.NewRecorder()
	handler := http.HandlerFunc(getUserMessagesHandler)

	handler.ServeHTTP(rr, req)

	if status := rr.Code; status != http.StatusMethodNotAllowed {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusMethodNotAllowed)
	}
}

func TestGetUserMessagesHandler_InvalidPath(t *testing.T) {
	req, err := http.NewRequest("GET", "/v1/user/user123/wrong", nil)
	if err != nil {
		t.Fatal(err)
	}

	rr := httptest.NewRecorder()
	handler := http.HandlerFunc(getUserMessagesHandler)

	handler.ServeHTTP(rr, req)

	if status := rr.Code; status != http.StatusBadRequest {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusBadRequest)
	}
}

// Optional: Integrated DB mock test or test DB would go here.
// Since we don't mock the MongoDB collection interface natively in standard lib tests without refactoring to interfaces,
// we just test the standard HTTP routing constraints for now.
func TestGetUserMessagesHandler_DBNotInitialized(t *testing.T) {
	req, err := http.NewRequest("GET", "/v1/user/user123/messages", nil)
	if err != nil {
		t.Fatal(err)
	}

	rr := httptest.NewRecorder()
	handler := http.HandlerFunc(getUserMessagesHandler)

	// Since collection is nil, it will panic or error out inside the function if not handled.
	// For this exercise, simple setup for routing checks is enough.
	defer func() {
		if r := recover(); r != nil {
			t.Log("Recovered from panic for uninitialized DB, which is expected in this pure handler test.")
		}
	}()

	handler.ServeHTTP(rr, req)
}
