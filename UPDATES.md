# Monitoring App - Simplified to Video Recording Only

## Changes Made

### Removed Features
- ❌ App monitoring/tracking system
- ❌ App database (Room, entities, DAOs)
- ❌ App vault/import system
- ❌ LauncherScreen with app grid

### New Features
- ✅ **Screen Recording** - Records device screen continuously
- ✅ **Front Camera Recording** - Records front-facing camera for selfie view
- ✅ **Automatic Start** - Both recordings start automatically when service initializes
- ✅ **Encrypted Storage** - All videos encrypted with AES-256-GCM, requires PIN to access
- ✅ **Recording Vault** - View and manage encrypted recordings (PIN: 1234)

## How It Works

1. **Background Service**
   - Starts automatically on phone unlock
   - Runs as foreground service (visible notification)
   - Records continuously in background

2. **Recording Types**
   - **Screen Recording**: Full device screen at 1080x1920, 30fps, 2.5Mbps bitrate
   - **Camera Recording**: Front camera at 720x1280, 30fps, includes audio

3. **Encryption & Storage**
   - Videos stored in app's private encrypted directory
   - Only unlockable with correct PIN (default: 1234)
   - Max storage: 10GB (auto-deletes oldest files)

4. **Access Vault**
   - Tap top-right corner 3 times to open Parent Portal
   - Enter PIN to access stored recordings
   - View file size and recording type
   - Delete individual recordings

## File Structure

```
src/main/java/com/example/myapplication/
├── MainActivity.kt              # Entry point, handles PIN unlock
├── service/
│   └── MonitoringService.kt     # Screen & camera recording service
├── security/
│   └── EncryptionManager.kt     # Encryption/decryption logic
└── ui/screens/
    ├── VaultScreen.kt          # Recordings vault UI
    └── ParentPortalScreen.kt    # Parent access portal
```

## Permissions Required

- `CAMERA` - Front camera access
- `RECORD_AUDIO` - Audio in recordings
- `FOREGROUND_SERVICE` - Background recording
- `FOREGROUND_SERVICE_CAMERA` - Camera service
- `FOREGROUND_SERVICE_MICROPHONE` - Audio service

## Security

- All videos encrypted with 256-bit AES-GCM
- Strong encryption keys stored in Android KeyStore
- PIN protection (4-digit)
- Private app directory (not accessible without decryption)

## Notes

- Default PIN is "1234" - Change in VaultScreen.kt
- Service runs on START_STICKY (restarts if killed)
- Notification title is "System Engine" (hidden)
- Max storage auto-triggers cleanup of oldest files
