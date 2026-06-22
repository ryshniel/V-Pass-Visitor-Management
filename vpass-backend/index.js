// index.js
require('dotenv').config(); // Load environment variables from .env
const express = require('express');
const cors = require('cors');

// NEW: Updated Firebase Modular Imports
const { initializeApp, cert } = require('firebase-admin/app');
const { getDatabase } = require('firebase-admin/database');

const cloudinary = require('cloudinary').v2;
const multer = require('multer');

const app = express();
const upload = multer({ storage: multer.memoryStorage() }); // Temporarily hold images in server memory

// Middleware
app.use(cors());
app.use(express.json());

// 1. Initialize Firebase Admin SDK using the new V12+ syntax
const serviceAccount = require("./serviceAccountKey.json");
initializeApp({
  credential: cert(serviceAccount),
  databaseURL: process.env.FIREBASE_DATABASE_URL
});
const db = getDatabase();

// 2. Initialize Cloudinary using your .env configuration string
cloudinary.config({
  cloudinary_url: process.env.CLOUDINARY_URL
});

// ==========================================
// ROUTE 1: System Health Check
// ==========================================
app.get('/api/health', (req, res) => {
    res.status(200).json({ status: "success", message: "V-PASS In-House API Engine Online!" });
});

// ==========================================
// ROUTE 2: Verify Visitor (Blacklist Check)
// Called by Android Guard Scan Dashboard
// ==========================================
app.get('/api/v1/visitor/verify/:uid', async (req, res) => {
    const visitorId = req.params.uid;
    try {
        const snapshot = await db.ref(`visitors/${visitorId}`).once('value');
        const visitorData = snapshot.val();

        if (!visitorData) {
            return res.status(404).json({ status: "denied", message: "Access Denied: Invalid QR code." });
        }

        if (visitorData.isBlacklisted === true || visitorData.isBlacklisted === "true") {
            return res.status(403).json({ status: "denied", message: "Access Denied: Banned by management." });
        }

        res.status(200).json({
            status: "approved",
            visitor_id: visitorId,
            name: visitorData.name || "Unknown",
            plate_number: visitorData.plateNumber || "N/A",
            message: "Access Approved. Welcome!",
            photo_url: visitorData.photoUrl || null
        });
    } catch (error) {
        console.error("Database Error:", error);
        res.status(500).json({ status: "error", message: "Internal Server Fault." });
    }
});

// ==========================================
// ROUTE 3: Register Visitor with Photo ID
// Handles direct upload to Cloudinary and saves URL to Firebase
// ==========================================
app.post('/api/v1/visitor/register', upload.single('idPhoto'), async (req, res) => {
    const { uid, name, plateNumber } = req.body;

    try {
        if (!req.file) {
            return res.status(400).json({ status: "failed", message: "Profile/ID photo file is required." });
        }

        // Upload the raw file buffer directly to Cloudinary
        const uploadResult = await new Promise((resolve, reject) => {
            const stream = cloudinary.uploader.upload_stream(
                { folder: "vpass_visitor_ids" },
                (error, result) => {
                    if (error) reject(error);
                    else resolve(result);
                }
            );
            stream.end(req.file.buffer);
        });

        // Save the resulting image URL along with text data to Firebase
        const newVisitorData = {
            name: name,
            plateNumber: plateNumber,
            isBlacklisted: false,
            photoUrl: uploadResult.secure_url // Cloudinary storage link
        };

        await db.ref(`visitors/${uid}`).set(newVisitorData);

        res.status(201).json({
            status: "success",
            message: "Visitor account registered and synced with cloud assets.",
            photoUrl: uploadResult.secure_url
        });

    } catch (error) {
        console.error("Upload/Registration Error:", error);
        res.status(500).json({ status: "error", message: "Failed to process remote image upload." });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`🚀 In-House Server standing by on port ${PORT}`));