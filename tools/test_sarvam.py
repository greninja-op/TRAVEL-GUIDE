import json
import urllib.request
import os

API_KEY = "sk_cvud4nm8_1RkO8MjGQNmFgyNaOQOTnFzV"

def test_tts_english():
    url = "https://api.sarvam.ai/text-to-speech"
    headers = {
        "api-subscription-key": API_KEY,
        "Content-Type": "application/json"
    }
    data = {
        "inputs": ["Welcome to Fort Kochi. St. Francis Church is the oldest European church in India."],
        "target_language_code": "en-IN",
        "speaker": "ritu",
        "model": "bulbul:v3"
    }
    req = urllib.request.Request(url, data=json.dumps(data).encode("utf-8"), headers=headers, method="POST")
    with urllib.request.urlopen(req) as resp:
        res = json.loads(resp.read().decode("utf-8"))
        assert "audios" in res and len(res["audios"]) > 0
        print("[PASS] Sarvam AI TTS (English en-IN) - status:", resp.status)
        return res["audios"][0]

def test_tts_malayalam():
    url = "https://api.sarvam.ai/text-to-speech"
    headers = {
        "api-subscription-key": API_KEY,
        "Content-Type": "application/json"
    }
    data = {
        "inputs": ["നമസ്കാരം, ഫോർട്ട് കൊച്ചിയിലേക്ക് സ്വാഗതം. നിങ്ങളുടെ സർവം എഐ യാത്രാ സഹായിയുടെ ശബ്ദ പരിശോധനയാണിത്."],
        "target_language_code": "ml-IN",
        "speaker": "ritu",
        "model": "bulbul:v3"
    }
    req = urllib.request.Request(url, data=json.dumps(data).encode("utf-8"), headers=headers, method="POST")
    with urllib.request.urlopen(req) as resp:
        res = json.loads(resp.read().decode("utf-8"))
        assert "audios" in res and len(res["audios"]) > 0
        print("[PASS] Sarvam AI TTS (Malayalam ml-IN) - status:", resp.status)

def test_tts_hindi():
    url = "https://api.sarvam.ai/text-to-speech"
    headers = {
        "api-subscription-key": API_KEY,
        "Content-Type": "application/json"
    }
    data = {
        "inputs": ["नमस्ते, फोर्ट कोच्चि में आपका स्वागत है। यह आपके सर्वम एआई यात्रा साथी का वॉयस टेस्ट है।"],
        "target_language_code": "hi-IN",
        "speaker": "ritu",
        "model": "bulbul:v3"
    }
    req = urllib.request.Request(url, data=json.dumps(data).encode("utf-8"), headers=headers, method="POST")
    with urllib.request.urlopen(req) as resp:
        res = json.loads(resp.read().decode("utf-8"))
        assert "audios" in res and len(res["audios"]) > 0
        print("[PASS] Sarvam AI TTS (Hindi hi-IN) - status:", resp.status)

def test_tts_tamil():
    url = "https://api.sarvam.ai/text-to-speech"
    headers = {
        "api-subscription-key": API_KEY,
        "Content-Type": "application/json"
    }
    data = {
        "inputs": ["வணக்கம், போர்ட் கொச்சிக்கு உங்களை வரவேற்கிறோம். இது உங்கள் சர்வம் ஏஐ பயண வழிகாட்டியின் குரல் சோதனை."],
        "target_language_code": "ta-IN",
        "speaker": "ritu",
        "model": "bulbul:v3"
    }
    req = urllib.request.Request(url, data=json.dumps(data).encode("utf-8"), headers=headers, method="POST")
    with urllib.request.urlopen(req) as resp:
        res = json.loads(resp.read().decode("utf-8"))
        assert "audios" in res and len(res["audios"]) > 0
        print("[PASS] Sarvam AI TTS (Tamil ta-IN) - status:", resp.status)

def test_stt(audio_b64):
    import base64
    audio_bytes = base64.b64decode(audio_b64)
    boundary = "----SarvamBoundary987654"
    headers = {
        "api-subscription-key": API_KEY,
        "Content-Type": f"multipart/form-data; boundary={boundary}"
    }
    
    parts = []
    def add_field(name, value):
        parts.append(f"--{boundary}\r\nContent-Disposition: form-data; name=\"{name}\"\r\n\r\n{value}\r\n".encode("utf-8"))
    
    add_field("model", "saaras:v3")
    add_field("language_code", "en-IN")
    
    file_header = f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"speech.wav\"\r\nContent-Type: audio/wav\r\n\r\n".encode("utf-8")
    file_footer = f"\r\n--{boundary}--\r\n".encode("utf-8")
    
    body = b"".join(parts) + file_header + audio_bytes + file_footer
    req = urllib.request.Request("https://api.sarvam.ai/speech-to-text", data=body, headers=headers, method="POST")
    with urllib.request.urlopen(req) as resp:
        res = json.loads(resp.read().decode("utf-8"))
        transcript = res.get("transcript", "")
        print("[PASS] Sarvam AI STT Transcription - status:", resp.status, "transcript:", transcript)

if __name__ == "__main__":
    print("Testing Sarvam AI Multi-Language Neural Speech Synthesis...")
    audio = test_tts_english()
    test_tts_malayalam()
    test_tts_hindi()
    test_tts_tamil()
    test_stt(audio)
    print("ALL 4 SARVAM AI INDIC LANGUAGES VERIFIED SUCCESSFULLY!")
