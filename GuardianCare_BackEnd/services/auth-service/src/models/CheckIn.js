const mongoose = require("mongoose");

const CheckInSchema = new mongoose.Schema({
  elderly_id: { 
    type: String, 
    ref: "User", 
    required: true 
  },
  summary: { 
    type: String 
  },
  priority: { 
    type: Number, 
    default: 0 
  },
  mood: { 
    type: Number 
  },
  status: { 
    type: String 
  },
  transcript: { 
    type: String 
  },
  created_at: { 
    type: Date, 
    default: Date.now 
  },
  updated_at: { 
    type: Date, 
    default: Date.now 
  }
});

// Explicitly set the collection name to 'checkin' (singular) to match the MongoDB collection
module.exports = mongoose.model("CheckIn", CheckInSchema, "checkin");
