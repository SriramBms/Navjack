package de.tudarmstadt.tk.carsensing.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

/**
 * Class to handle the local SQLite Database (not currently used)
 * @author Julien Gedeon
 *
 */


public class Database extends SQLiteOpenHelper {

	private static final String TAG = "Database";
	
	private static final String DATABASE_NAME = "carsensing_db.db";
	private static final int DATABASE_VERSION = 3;
	private static final String TABLE_MEASUREMENTS = "measurements";
	private static final String TABLE_LOCATION = "location";
	private SQLiteStatement insertData = null;
	private SQLiteStatement insertLocation = null;
	private SQLiteDatabase database = null;
	private static Database db = null;
	
	
	/**
	 * Lock the database
	 */
	private Boolean locked;

	
	/**
	 * Get database
	 * @return
	 */
	public static Database getDatabase() {
		return db;
	}

	/**
	 * Init database if it does not exist
	 * @param context
	 */
	public static void initDatabase(Context context) {
		Log.d(TAG, "Init Database");
		if (db == null) {
			db = new Database(context);	
		} else {
			db.close();
			db = null;
			db = new Database(context);
		}
					
	}
	
	/**
	 * Constructor
	 * @param context
	 */
	private Database(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		Log.d(TAG, "Database Constructor");
		database = getWritableDatabase();
		
		insertData = database.compileStatement("INSERT INTO " + TABLE_MEASUREMENTS + 
		" (phoneID, timestamp, type, value, description)" + 
		"VALUES (?, ?, ?, ?, ?)");

		insertLocation = database.compileStatement("INSERT INTO " + TABLE_LOCATION + 
				" (phoneID, timestamp, latitude, longitude, altitude, accuracy, provider)" + 
				"VALUES (?, ?, ?, ?, ?, ?, ?)");
		
		locked = false;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG, "Database onCreate");
		db.execSQL("CREATE TABLE "+ TABLE_MEASUREMENTS + 
				" (measurementID INTEGER PRIMARY KEY, " +
				"phoneID TEXT, " +				
				"timestamp INTEGER, " +
				"type TEXT, " +				
				"value REAL, " +
				"description TEXT " +
				")");
		
		
		db.execSQL("CREATE TABLE "+ TABLE_LOCATION + 
				" (locationID INTEGER PRIMARY KEY, " +
				"phoneID TEXT," +
				"timestamp INTEGER," +
				"latitude REAL, " +				
				"longitude REAL, " +
				"altitude REAL, " +				
				"accuracy REAL, " +
				"provider TEXT)");	
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(TAG, "Database onUpgrade");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEASUREMENTS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATION);
		onCreate(db);
	}
	
	/**
	 * Inserts new measurement data
	 * @param phoneID
	 * @param timestamp
	 * @param type
	 * @param value
	 * @param locationID
	 * @return measurementID
	 */
	public long addData(String phoneID, long timestamp, String type, double value, String description) {
		if(db != null && database.isOpen()){
			insertData.clearBindings();
			insertData.bindString(1, phoneID);
			insertData.bindLong(2, Long.valueOf(timestamp));
			insertData.bindString(3, type);
			insertData.bindDouble(4, value);
			insertData.bindString(5, description);
			long id =  insertData.executeInsert();
			Log.d(TAG, "SQLITE INSERT DATA, measurementID:" + id +  ", phoneID:" + phoneID + 
								", timestamp:" + timestamp + ", type:" + type + ", value:" + value + 
								", description:" + description);		
			return id;
		} else return 0;
	}
	
	/**
	 * Inserts a new location
	 * @param latitude
	 * @param longitude
	 * @param altitude
	 * @param accuracy
	 * @param provider
	 * @return locationID
	 */
	public long addLocation(String phoneID, long timestamp, double latitude, double longitude, double altitude, float accuracy, String provider) {
		if(db != null && database.isOpen()){
			insertLocation.clearBindings();
			insertLocation.bindString(1, phoneID);
			insertLocation.bindLong(2, Long.valueOf(timestamp));
			insertLocation.bindDouble(3, latitude);
			insertLocation.bindDouble(4, longitude);			
			insertLocation.bindDouble(5, altitude);		
			insertLocation.bindDouble(6, accuracy);
			insertLocation.bindString(7, provider);
			long id =  insertLocation.executeInsert();
			Log.d(TAG, "SQLITE INSERT LOCATION, locationID:" + id + ", phoneID:" + phoneID + 
					", timestamp:" + timestamp + ", latitude:" + latitude + ", longitude:" + longitude + 
					", altitude:" + altitude + ", accuracy:" + accuracy + ", provider:" + provider);
			return id;
		}else return 0;
	}
	

	/**
	 * 
	 * Deletes databse rows with timestamp < lastTimestamp
	 * @param lastTimestamp
	 */
	 public void removeData(long lastTimestamp) {	
		 if(db != null && database.isOpen()){
			 Log.d(TAG, "Database remove; lastTimestamp = " + lastTimestamp);
			Cursor measurementCursor = database.rawQuery("SELECT locationID FROM " + TABLE_MEASUREMENTS +
														" WHERE timestamp <= " + lastTimestamp, null);
			measurementCursor.moveToFirst();
			for (int i = 0; i < measurementCursor.getCount(); i++) {
				database.execSQL("DELETE FROM " + TABLE_LOCATION + " WHERE locationID = " + 
								measurementCursor.getInt(0));
				measurementCursor.moveToNext();
			}
			measurementCursor.close();
			database.execSQL("DELETE FROM " + TABLE_MEASUREMENTS + " WHERE timestamp <= " + lastTimestamp + ""); 
		 }
	}

	
	/**
	 * Delets all data from the database
	 */
	public void clearDatabase() {
		if(db != null && database.isOpen()){
			database.execSQL("DELETE FROM " + TABLE_MEASUREMENTS);
			database.execSQL("DELETE FROM " + TABLE_LOCATION);
			Log.d(TAG, "Database cleared");
		}
	}
	
	/**
	 * Lock database
	 */
	public void lock(){
		locked = true;
	}
	
	/**
	 * Unlock database
	 */
	public void unlock(){
		locked = false;
	}
	
	/**
	 * Determine if database is locked
	 * @return locked
	 */
	public Boolean isLocked(){
		return locked;
	}
}
