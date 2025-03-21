const mongoose = require("mongoose");

const UserSchema = new mongoose.Schema({
  user_type: { type: String, enum: ["elderly", "caretaker"], required: true }, 
  name: { type: String, required: true },
  email: { type: String, unique: true, required: true },
  password_hash: { type: String, required: true },
  dob: { type: Date, required: function () { return this.user_type === "elderly"; } }, // Required for elderly
  address: { type: String },
  medical_history: { type: [String] }, // Only for elderly
  point_of_contact: {
    name: { type: String },
    phone: { type: String },
    relationship: { type: String }
  },
  caretaker_id: { type: mongoose.Schema.Types.ObjectId, ref: "User", default: null }, // Nullable caretaker_id
  created_at: { type: Date, default: Date.now },
  updated_at: { type: Date, default: Date.now }
});

module.exports = mongoose.model("User", UserSchema);