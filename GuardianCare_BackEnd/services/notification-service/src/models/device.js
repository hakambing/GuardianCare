const mongoose = require('mongoose');

const deviceSchema = new mongoose.Schema({
  userId: {
    type: String,
    required: true
  },
  deviceToken: {
    type: String,
    required: true
  },
  deviceType: {
    type: String,
    enum: ['android', 'ios', 'web'],
    required: true
  },
  createdAt: {
    type: Date,
    default: Date.now
  }
});

// Compound index to ensure uniqueness of userId + deviceToken
deviceSchema.index({ userId: 1, deviceToken: 1 }, { unique: true });

const Device = mongoose.model('Device', deviceSchema);

module.exports = Device;
