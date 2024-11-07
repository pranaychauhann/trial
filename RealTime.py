import numpy as np
import os
import cv2
import mediapipe as mp
from tensorflow.keras.models import load_model
from KeypointsExtraction import draw_landmarks, image_process, keypoint_extraction
import time

# Path to data and actions defined during training
PATH = os.path.join('data')
actions = np.array(os.listdir(PATH))

# Load the trained model
model = load_model('my_model.h5')

# Path to the specific video file
video_path = r"C:\Users\chauh\Desktop\dummy\uploads\video.mp4"

# Initialize prediction and sentence-related lists
sentence, keypoints, last_prediction = [], [], None
cooldown_frames, cooldown_threshold = 0, 20  # Cooldown period of 20 frames after each prediction

# Initialize MediaPipe holistic model
with mp.solutions.holistic.Holistic(min_detection_confidence=0.80, min_tracking_confidence=0.80) as holistic:
    while True:
        # Check if the video file exists
        if os.path.exists(video_path):
            cap = cv2.VideoCapture(video_path)

            if not cap.isOpened():
                print("Cannot access video.")
                break

            while cap.isOpened():
                ret, image = cap.read()
                if not ret:
                    break
                
                # Process frame and extract keypoints
                results = image_process(image, holistic)
                draw_landmarks(image, results)
                keypoints.append(keypoint_extraction(results))

                # Predict every 20 frames if cooldown is not active
                if len(keypoints) == 20 and cooldown_frames == 0:
                    keypoints = np.array(keypoints)
                    prediction = model.predict(keypoints[np.newaxis, :, :])
                    keypoints = []

                    # Check if the prediction exceeds threshold
                    if np.max(prediction) >= 0.99:
                        predicted_action = actions[np.argmax(prediction)]
                        
                        # Only append if prediction differs from the last one
                        if predicted_action != last_prediction:
                            sentence.append(predicted_action)
                            last_prediction = predicted_action
                            cooldown_frames = cooldown_threshold  # Activate cooldown

                # Decrease cooldown frame count
                cooldown_frames = max(0, cooldown_frames - 1)

                # Limit sentence length
                if len(sentence) > 7:
                    sentence = sentence[-7:]

            # Print the final sentence detected after processing the video
            final_sentence = ' '.join([word.capitalize() for word in sentence])
            print("Detected Sentence:", final_sentence)

            # Clear sentence for next video and release resources
            sentence.clear()
            cap.release()
            cv2.destroyAllWindows()
            
            # Delete the video file after processing
            os.remove(video_path)
            print("Processed and deleted the video file.")
        
        else:
            # Wait and check again after a short delay if no video is found
            time.sleep(5)
