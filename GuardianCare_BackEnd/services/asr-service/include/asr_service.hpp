#pragma once
#include <string>

// Forward declaration to store a pointer to the whisper context in your class:
struct whisper_context;

class ASRService {
    public:
        // Initialize with path to your .bin Whisper model
        ASRService(const std::string& modelPath);
    
        // Transcribe a .wav file and return a transcript
        std::string transcribe(const std::string& wavPath);
    
        // Clean up
        ~ASRService();
    
    private:
        // path to .bin model
        std::string modelPath_;
    
        // pointer to the whisper context (allocated by whisper_init_from_file)
        whisper_context* ctx_ = nullptr;
    };