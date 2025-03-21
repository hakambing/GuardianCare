/**
 * Script to add test check-in data to the database
 * 
 * Usage: 
 * 1. Make sure MongoDB is running
 * 2. Run this script with Node.js:
 *    node src/scripts/addTestCheckInData.js
 */

const mongoose = require('mongoose');
require('dotenv').config();

// Import the CheckIn model
const CheckIn = require('../models/CheckIn');

// Connect to MongoDB
const connectDB = async () => {
  try {
    // Use the specific MongoDB URI from the .env file
    const mongoURI = 'mongodb+srv://GuardianCare:9csWq9hsh4o3UoOe@guardiancare.ktqq1.mongodb.net/GuardianCare';
    
    console.log(`Attempting to connect to MongoDB Atlas at: ${mongoURI}`);
    
    await mongoose.connect(mongoURI, {
      useNewUrlParser: true,
      useUnifiedTopology: true,
      serverSelectionTimeoutMS: 10000 // 10 second timeout
    });
    
    console.log(`✅ MongoDB Connected: ${mongoose.connection.host}`);
    return true;
  } catch (error) {
    console.error(`❌ Error connecting to MongoDB: ${error.message}`);
    console.error(`Stack trace: ${error.stack}`);
    
    // Try alternative connection string
    try {
      console.log('Trying alternative connection to MongoDB...');
      const altMongoURI = 'mongodb://localhost:27017/GuardianCare';
      
      await mongoose.connect(altMongoURI, {
        useNewUrlParser: true,
        useUnifiedTopology: true,
        serverSelectionTimeoutMS: 5000
      });
      
      console.log(`✅ MongoDB Connected (alternative): ${mongoose.connection.host}`);
      return true;
    } catch (altError) {
      console.error(`❌ Alternative connection also failed: ${altError.message}`);
      
      // Try one more connection string
      try {
        console.log('Trying one more connection to MongoDB...');
        const localMongoURI = 'mongodb://127.0.0.1:27017/GuardianCare';
        
        await mongoose.connect(localMongoURI, {
          useNewUrlParser: true,
          useUnifiedTopology: true,
          serverSelectionTimeoutMS: 5000
        });
        
        console.log(`✅ MongoDB Connected (local): ${mongoose.connection.host}`);
        return true;
      } catch (localError) {
        console.error(`❌ All connection attempts failed`);
        process.exit(1);
      }
    }
  }
};

// Test data
const testCheckIns = [
  {
    elderly_id: '67c88cf506028f44fbe715aa', // Replace with actual elderly ID
    summary: 'The user reports feeling good, did some gardening, and had a nice lunch.',
    priority: 0,
    mood: 2,
    status: 'fine',
    transcript: 'Today I did some gardening. Then I went to have lunch with my husband.',
    created_at: new Date(),
    updated_at: new Date()
  },
  {
    elderly_id: '67c88cf506028f44fbe715aa', // Replace with actual elderly ID
    summary: 'The user reports feeling sad, did some reading, and had a quiet day.',
    priority: 1,
    mood: -1,
    status: 'sad',
    transcript: 'Today I felt a bit down. I read a book and stayed home most of the day.',
    created_at: new Date(Date.now() - 24 * 60 * 60 * 1000), // 1 day ago
    updated_at: new Date(Date.now() - 24 * 60 * 60 * 1000)
  },
  {
    elderly_id: '67c88cf506028f44fbe715aa', // Replace with actual elderly ID
    summary: 'The user reports feeling happy, went for a walk, and met with friends.',
    priority: 0,
    mood: 3,
    status: 'happy',
    transcript: 'Today was a great day! I went for a walk in the park and met with my friends for coffee.',
    created_at: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000), // 2 days ago
    updated_at: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000)
  }
];

// Add test data to the database
const addTestData = async () => {
  try {
    // Clear existing check-ins for this elderly
    const elderlyId = testCheckIns[0].elderly_id;
    console.log(`Attempting to clear existing check-ins for elderly: ${elderlyId}`);
    
    const deleteResult = await CheckIn.deleteMany({ elderly_id: elderlyId });
    console.log(`✅ Cleared ${deleteResult.deletedCount} existing check-ins for elderly: ${elderlyId}`);
    
    // Insert new check-ins
    console.log(`Inserting ${testCheckIns.length} test check-ins...`);
    const result = await CheckIn.insertMany(testCheckIns);
    console.log(`✅ Added ${result.length} test check-ins to the database`);
    
    // Log the inserted check-ins
    console.log('Check-ins added:');
    result.forEach(checkIn => {
      console.log(`- ID: ${checkIn._id}, Elderly ID: ${checkIn.elderly_id}, Status: ${checkIn.status}, Created: ${checkIn.created_at}`);
    });
    
    // Verify that the check-ins can be retrieved
    console.log(`Verifying check-ins can be retrieved...`);
    const retrievedCheckIns = await CheckIn.find({ elderly_id: elderlyId }).sort({ created_at: -1 });
    console.log(`✅ Retrieved ${retrievedCheckIns.length} check-ins for elderly: ${elderlyId}`);
    
    // Try different query approaches to ensure our robust query methods work
    console.log(`Testing case-insensitive query...`);
    const caseInsensitiveCheckIns = await CheckIn.find({ 
      elderly_id: { $regex: new RegExp('^' + elderlyId + '$', 'i') } 
    });
    console.log(`✅ Case-insensitive query found ${caseInsensitiveCheckIns.length} check-ins`);
    
    // Close the database connection
    console.log('Closing database connection...');
    await mongoose.connection.close();
    console.log('✅ Database connection closed');
    
    console.log('\n✅✅✅ TEST DATA ADDED SUCCESSFULLY ✅✅✅');
    console.log('You should now be able to see check-in data in the app.');
  } catch (error) {
    console.error(`❌ Error adding test data: ${error.message}`);
    console.error(`Stack trace: ${error.stack}`);
    
    // Try to close the connection even if there was an error
    try {
      await mongoose.connection.close();
      console.log('Database connection closed after error');
    } catch (closeError) {
      console.error(`Error closing database connection: ${closeError.message}`);
    }
    
    process.exit(1);
  }
};

// Run the script
connectDB().then(() => {
  addTestData();
});
