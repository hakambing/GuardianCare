/**
 * Simple script to test MongoDB connection
 */

const mongoose = require('mongoose');
require('dotenv').config();

// Test MongoDB connection
async function testConnection() {
  console.log('Starting MongoDB connection test...');
  
  // Try multiple connection strings
  const connectionStrings = [
    {
      name: 'MongoDB Atlas',
      uri: 'mongodb+srv://GuardianCare:9csWq9hsh4o3UoOe@guardiancare.ktqq1.mongodb.net/GuardianCare'
    },
    {
      name: 'Local MongoDB',
      uri: 'mongodb://localhost:27017/GuardianCare'
    },
    {
      name: 'Local MongoDB (IP)',
      uri: 'mongodb://127.0.0.1:27017/GuardianCare'
    }
  ];
  
  for (const connection of connectionStrings) {
    try {
      console.log(`Attempting to connect to ${connection.name} at: ${connection.uri}`);
      
      await mongoose.connect(connection.uri, {
        useNewUrlParser: true,
        useUnifiedTopology: true,
        serverSelectionTimeoutMS: 5000
      });
      
      console.log(`✅ Successfully connected to ${connection.name}`);
      console.log(`Host: ${mongoose.connection.host}`);
      console.log(`Database name: ${mongoose.connection.name}`);
      
      // List all collections
      console.log('Listing collections:');
      const collections = await mongoose.connection.db.listCollections().toArray();
      collections.forEach(collection => {
        console.log(`- ${collection.name}`);
      });
      
      // Close connection
      await mongoose.connection.close();
      console.log(`Connection to ${connection.name} closed`);
      
      // Exit with success
      console.log('Connection test successful!');
      process.exit(0);
    } catch (error) {
      console.error(`❌ Failed to connect to ${connection.name}: ${error.message}`);
      
      // Continue to the next connection string
      await mongoose.connection.close().catch(() => {});
    }
  }
  
  console.error('❌ All connection attempts failed');
  process.exit(1);
}

// Run the test
testConnection().catch(error => {
  console.error('Unhandled error:', error);
  process.exit(1);
});
