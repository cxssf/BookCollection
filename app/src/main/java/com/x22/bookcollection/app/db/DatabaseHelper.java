package com.x22.bookcollection.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.x22.bookcollection.app.model.BookItem;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "BookCollection.db";

    private static final String CREATE_TABLE_BOOKS =
            "CREATE TABLE " + BookEntry.TABLE_NAME + " (" +
                    BookEntry._ID + " INTEGER PRIMARY KEY," +
                    BookEntry.COLUMN_NAME_TITLE + " TEXT," +
                    BookEntry.COLUMN_NAME_AUTHOR  + " TEXT" +
            ")";

    private static final String DELETE_TABLE_BOOKS = "DROP TABLE IF EXISTS " + BookEntry.TABLE_NAME;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // creating required tables
        db.execSQL(CREATE_TABLE_BOOKS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // on upgrade drop older tables
        db.execSQL("DROP TABLE IF EXISTS " + DELETE_TABLE_BOOKS);

        // create new tables
        onCreate(db);
    }

    public static abstract class BookEntry implements BaseColumns {
        public static final String TABLE_NAME = "books";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_AUTHOR = "author";
    }

    public long createBook(BookItem bookItem) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(BookEntry.COLUMN_NAME_TITLE, bookItem.getTitle());
        values.put(BookEntry.COLUMN_NAME_AUTHOR, bookItem.getAuthor());

        return db.insert(BookEntry.TABLE_NAME, null, values);
    }

    public List<BookItem> getAllBooks() {
        List<BookItem> books = new ArrayList<BookItem>();

        SQLiteDatabase db = this.getReadableDatabase();
        //Cursor cursor = db.rawQuery("SELECT * FROM " + BookEntry.TABLE_NAME, null);

        String[] columns = {
                BookEntry._ID,
                BookEntry.COLUMN_NAME_TITLE,
                BookEntry.COLUMN_NAME_AUTHOR
        };

        String sortOrder = BookEntry.COLUMN_NAME_TITLE + " ASC";

        Cursor cursor = db.query(BookEntry.TABLE_NAME, columns, null, null, null, null, null);

        if(cursor.moveToFirst()) {
            do {
                BookItem bookItem = new BookItem();
                bookItem.setId(cursor.getInt(cursor.getColumnIndex(BookEntry._ID)));
                bookItem.setTitle(cursor.getString(cursor.getColumnIndex(BookEntry.COLUMN_NAME_TITLE)));
                bookItem.setAuthor(cursor.getString(cursor.getColumnIndex(BookEntry.COLUMN_NAME_AUTHOR)));

                books.add(bookItem);
            } while(cursor.moveToNext());
        }

        db.close();

        return books;
    }

    public void deleteAllBooks() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(BookEntry.TABLE_NAME, null, null);
        db.close();
    }

    public void closeDb() {
        SQLiteDatabase db = this.getReadableDatabase();
        if (db != null && db.isOpen()) {
            db.close();
        }
    }
}
