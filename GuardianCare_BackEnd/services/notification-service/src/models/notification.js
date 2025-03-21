const mongoose = require('mongoose');

const NotificationType = {
  FALL_DETECTION: 'FALL_DETECTION',
  DEVICE_STATUS: 'DEVICE_STATUS'
};

const notificationSchema = new mongoose.Schema({
  type: {
    type: String,
    required: true,
    enum: Object.values(NotificationType)
  },
  elderlyId: {
    type: String,
    required: true
  },
  priority: {
    type: String,
    required: true,
    enum: ['LOW', 'MEDIUM', 'HIGH']
  },
  timestamp: {
    type: Date,
    default: Date.now
  },
  content: {
    title: {
      type: String,
      required: true
    },
    message: {
      type: String,
      required: true
    },
    elderlyName: {
      type: String,
      default: "Unknown"
    },
    data: {
      type: mongoose.Schema.Types.Mixed,
      default: {}
    }
  },
  recipients: [{
    userId: {
      type: String,
      required: true
    },
    role: {
      type: String,
      required: true,
      enum: ['CARETAKER', 'ADMIN']
    },
    deviceTokens: {
      type: [String],
      default: []
    },
    notificationSent: {
      type: Boolean,
      default: false
    },
    readTimestamp: {
      type: Date
    }
  }]
}, {
  timestamps: true
});

// Indexes
notificationSchema.index({ elderlyId: 1 });
notificationSchema.index({ 'recipients.userId': 1 });
notificationSchema.index({ timestamp: -1 });

const Notification = mongoose.model('Notification', notificationSchema);

module.exports = {
  Notification,
  NotificationType
};
