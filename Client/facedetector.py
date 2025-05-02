import cv2

# Load the Haar Cascade face detection model from OpenCV
face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')

# Open the cam's stream
cap = cv2.VideoCapture("http://192.168.1.23:8989")  # 0 means default webcam

if not cap.isOpened():
    print("Error: Cannot open webcam")
    exit()

while True:
    # Capture frame-by-frame
    ret, frame = cap.read()
    
    # If the frame was not grabbed, break the loop
    if not ret:
        print("Failed to grab frame")
        break

    # Convert frame to grayscale (Haar Cascade works with grayscale images)
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)

    # Detect faces in the grayscale image
    faces = face_cascade.detectMultiScale(gray, scaleFactor=1.05, minNeighbors=7, minSize=(50, 50))

    # Draw bounding boxes around detected faces
    for (x, y, w, h) in faces:
        cv2.rectangle(frame, (x, y), (x + w, y + h), (255, 0, 0), 2)

    # Display the resulting frame
    cv2.imshow("Face Detection", frame)

    # Press 'q' to exit
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

# Release resources and close the window
cap.release()
cv2.destroyAllWindows()
