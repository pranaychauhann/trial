from flask import Flask, request, jsonify, send_from_directory
import os

app = Flask(__name__)

# Create a directory for uploaded files
UPLOAD_FOLDER = 'uploads'  # Make sure this directory exists
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)

allowed_extensions = {'mp4', 'avi', 'mov', 'mkv'}  # Allowed video formats

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in allowed_extensions

@app.route('/upload', methods=['POST'])
def upload_file():
    if 'file' not in request.files:
        return jsonify({'error': 'No file part'}), 400
    file = request.files['file']
    if file.filename == '':
        return jsonify({'error': 'No selected file'}), 400
    if not allowed_file(file.filename):
        return jsonify({'error': 'File type not allowed'}), 400

    # Save the file to the uploads folder
    file.save(os.path.join(UPLOAD_FOLDER, file.filename))
    return jsonify({'message': 'File uploaded successfully', 'filename': file.filename, 'file_path': os.path.join(UPLOAD_FOLDER, file.filename)}), 200

@app.route('/uploads', methods=['GET'])
def list_files():
    files = os.listdir(UPLOAD_FOLDER)
    return jsonify({'files': files}), 200

@app.route('/video/<filename>', methods=['GET'])
def get_video(filename):
    return send_from_directory(UPLOAD_FOLDER, filename)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)  # Enable debug mode for easier troubleshooting
