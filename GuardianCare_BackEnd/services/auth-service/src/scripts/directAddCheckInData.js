/**
 * Script to directly add check-in data to the MongoDB Atlas database
 */

const { MongoClient } = require('mongodb');

// MongoDB Atlas connection string
const uri = 'mongodb+srv://GuardianCare:9csWq9hsh4o3UoOe@guardiancare.ktqq1.mongodb.net/GuardianCare';

// Test data
const testCheckIns = [
  {
    elderly_id: '67c88cf506028f44fbe715aa', // The elderly ID from your logcat
    summary: 'The user reports feeling good, did some gardening, and had a nice lunch.',
    priority: 0,
    mood: 2,
    status: 'fine',
    transcript: 'Today I did some gardening. Then I went to have lunch with my husband.',
    created_at: new Date(),
    updated_at: new Date()
  },
  {
    elderly_id: '67c88cf506028f44fbe715aa',
    summary: 'The user reports feeling sad, did some reading, and had a quiet day.',
    priority: 1,
    mood: -1,
    status: 'sad',
    transcript: 'Today I felt a bit down. I read a book and stayed home most of the day.',
    created_at: new Date(Date.now() - 24 * 60 * 60 * 1000), // 1 day ago
    updated_at: new Date(Date.now() - 24 * 60 * 60 * 1000)
  },
  {
    elderly_id: '67c88cf506028f44fbe715aa',
    summary: 'The user reports feeling happy, went for a walk, and met with friends.',
    priority: 0,
    mood: 3,
    status: 'happy',
    transcript: 'Today was a great day! I went for a walk in the park and met with my friends for coffee.',
    created_at: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000), // 2 days ago
    updated_at: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000)
  }
];

async function addCheckInData() {
  console.log('Starting direct MongoDB connection...');
  
  let client;
  try {
    // Connect to MongoDB Atlas
    client = new MongoClient(uri, { useNewUrlParser: true, useUnifiedTopology: true });
    await client.connect();
    console.log('Connected to MongoDB Atlas');
    
    // Get the database and collection
    const database = client.db('GuardianCare');
    // Use the correct collection name 'checkin' (singular) as specified by the user
    const collection = database.collection('checkin');
    
    // Log all collections to verify the correct collection name
    console.log('Listing all collections in the database:');
    const allCollections = await database.listCollections().toArray();
    allCollections.forEach(coll => {
      console.log(`- ${coll.name}`);
    });
    
    // Clear existing check-ins for this elderly
    const elderlyId = testCheckIns[0].elderly_id;
    console.log(`Clearing existing check-ins for elderly: ${elderlyId}`);
    const deleteResult = await collection.deleteMany({ elderly_id: elderlyId });
    console.log(`Cleared ${deleteResult.deletedCount} existing check-ins`);
    
    // Insert new check-ins
    console.log(`Inserting ${testCheckIns.length} test check-ins...`);
    const result = await collection.insertMany(testCheckIns);
    console.log(`Inserted ${result.insertedCount} check-ins`);
    
    // Verify that the check-ins were inserted
    const checkIns = await collection.find({ elderly_id: elderlyId }).toArray();
    console.log(`Found ${checkIns.length} check-ins for elderly: ${elderlyId}`);
    checkIns.forEach(checkIn => {
      console.log(`- ID: ${checkIn._id}, Status: ${checkIn.status}, Created: ${checkIn.created_at}`);
    });
    
    // List all collections in the database
    console.log('Listing all collections in the database:');
    const collections = await database.listCollections().toArray();
    collections.forEach(collection => {
      console.log(`- ${collection.name}`);
    });
    
    console.log('Check-in data added successfully!');
  } catch (error) {
    console.error('Error:', error);
  } finally {
    // Close the connection
    if (client) {
      await client.close();
      console.log('MongoDB connection closed');
    }
  }
}

// Run the script
addCheckInData().catch(console.error);
