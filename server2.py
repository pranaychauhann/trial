from flask import Flask, request, jsonify
import os

app = Flask(__name__)

# Create the 'uploads' directory if it doesn't exist
UPLOAD_FOLDER = 'uploads'
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)

# Route for handling video upload and returning output.txt content
@app.route('/upload', methods=['POST'])
def upload_video():
    # Check if a file is included in the request
    if 'file' not in request.files:
        return jsonify({'error': 'No file part'}), 400

    video_file = request.files['file']

    # Check if the file has a valid name
    if video_file.filename == '':
        return jsonify({'error': 'No selected file'}), 400

    # Save the video file temporarily (adjust path as necessary)
    video_path = os.path.join(UPLOAD_FOLDER, video_file.filename)
    video_file.save(video_path)
    
    # Log the file saving
    app.logger.info(f"Video file saved at {video_path}")

    # Now read the content of output.txt
    try:
        output_file_path = r'C:\Users\chauh\Desktop\dummy\output.txt'
        with open(output_file_path, 'r') as file:
            output_text = file.read()
    except FileNotFoundError:
        return jsonify({'error': 'Output file not found'}), 500
    except Exception as e:
        app.logger.error(f"Error reading output.txt: {str(e)}")
        return jsonify({'error': 'Error reading output file'}), 500

    # Return the content of output.txt as a response
    return jsonify({'outputText': output_text})

# Route for fetching output (simulated)
@app.route('/output', methods=['GET'])
def get_output():
    try:
        output_file_path = r'C:\Users\chauh\Desktop\dummy\output.txt'
        with open(output_file_path, 'r') as file:
            output_text = file.read()
    except FileNotFoundError:
        return jsonify({'error': 'Output file not found'}), 500
    except Exception as e:
        app.logger.error(f"Error reading output.txt: {str(e)}")
        return jsonify({'error': 'Error reading output file'}), 500

    # Return the content of output.txt as a response
    return jsonify({'outputText': output_text})

if __name__ == '__main__':
    # Run the Flask server
    app.run(debug=True, host='0.0.0.0', port=5000)
