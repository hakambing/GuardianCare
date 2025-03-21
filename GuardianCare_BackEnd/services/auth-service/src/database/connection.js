const mongoose = require('mongoose');

const connectDB = async () => {
  try {
    const mongoURI = process.env.MONGODB_URI || 'mongodb://localhost:27017/guardiancare';
    
    const conn = await mongoose.connect(mongoURI, {
      // MongoDB driver's Unified Topology is enabled by default in Mongoose 6+
    });

    console.log(`✅ MongoDB Connected: ${conn.connection.host}`);
  } catch (error) {
    console.error(`❌ Error connecting to MongoDB: ${error.message}`);
    process.exit(1); // Exit with failure
  }
};

module.exports = connectDB;
