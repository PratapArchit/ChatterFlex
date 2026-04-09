#include <WiFi.h>
#include <HTTPClient.h>

const char* ssid = "OnePlus 11R 5G";
const char* password = "xwcl7064ap1982";
const char* serverURL = "https://chatterflex.onrender.com/setmessage";

const int flexPins[5] = {36, 39, 34, 35, 32};  

struct GestureRange {
  int minVals[5];
  int maxVals[5];
  const char* message;
};

GestureRange knownGestures[] = {
  {{0, 1200, 0, 0, 0}, {1000, 4500, 1000, 1000, 1000}, "Hello there, how are you ?"},
  {{0, 1200, 1200, 1200, 0}, {1000, 4500, 4500, 4500, 1000}, "I am all good."},
  {{0, 1200, 1200, 1200, 1200},  {1000, 4500, 4500, 4500, 4500}, "This is a medical emergency."},
  {{1200,0, 0, 0, 0},  {4500, 1000, 1000, 1000, 1000}, "I need water"},
  {{0,0, 0, 0, 0},  {1000, 1000, 1000, 1000, 1000}, "Sir is Awesome"},
  {{1200,1200, 0, 0, 1200},  {4500, 4500, 1000, 1000, 4500}, "Lets go party"},
  {{1200,1200, 0, 0, 0},  {4500, 4500, 1000, 1000, 1000}, "Have a nice day"},
  {{0,1200, 0, 0, 1200},  {1000, 4500, 1000, 1000, 4500}, "Where is my phone"},
  {{0,1200, 1200, 0, 1200},  {1000, 4500, 4500, 1000, 4500}, "Need a doctor"},
  {{0,0, 1200, 1200, 0},  {1000, 1000, 4500, 4500, 1000}, "Done with it"},
  
};

bool matchesGesture(int current[5], const GestureRange& gesture) {
  for (int i = 0; i < 5; i++) {
    if (current[i] < gesture.minVals[i] || current[i] > gesture.maxVals[i]) {
      return false;
    }
  }
  return true;
}

void sendToServer(String message) {
  if (WiFi.status() == WL_CONNECTED) {
    HTTPClient http;
    http.begin(serverURL);
    http.addHeader("Content-Type", "application/json");

    String payload = "{\"message\":\"" + message + "\"}";
    int httpResponseCode = http.POST(payload);

    Serial.print("Sent: "); Serial.println(payload);
    Serial.print("HTTP Response code: "); Serial.println(httpResponseCode);
    
    http.end();
  } else {
    Serial.println("WiFi not connected.");
  }
}

void setup() {
  Serial.begin(115200);

  for (int i = 0; i < 5; i++) {
    pinMode(flexPins[i], INPUT);
  }

  WiFi.begin(ssid, password);
  Serial.print("Connecting to WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println(" Connected!");
}

void loop() {
  int readings[5];
  for (int i = 0; i < 5; i++) {
    readings[i] = analogRead(flexPins[i]);
    Serial.print("Sensor "); Serial.print(i + 1);
    Serial.print(" (Pin "); Serial.print(flexPins[i]); Serial.print("): ");
    Serial.println(readings[i]);
    delay(10);
  }

  for (GestureRange& gesture : knownGestures) {
    if (matchesGesture(readings, gesture)) {
      Serial.println("Matched: " + String(gesture.message));
      sendToServer(gesture.message);
      delay(2000); 
      break;
    }
  }

  delay(1000); 
}
