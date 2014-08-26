package android.app.printerapp.devices;

import java.io.File;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.printerapp.ItemListActivity;
import android.app.printerapp.R;
import android.app.printerapp.StateUtils;
import android.app.printerapp.devices.database.DatabaseController;
import android.app.printerapp.model.ModelPrinter;
import android.app.printerapp.octoprint.OctoprintLoadAndPrint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.database.Cursor;
import android.util.Log;


/**
 * This class will handle list events such as add, remove or update
 * @author alberto-baeza
 *
 */
public class DevicesListController {
	
	//List for the printers found
	private static ArrayList<ModelPrinter> mList = new ArrayList<ModelPrinter>();
			
	//Add element to the list
	public static void addToList(ModelPrinter m){
		mList.add(m);
		
	}
	
	//Return the list
	public static ArrayList<ModelPrinter> getList(){
		
		return mList;
	}
	
	//Load device list from the Database
	public static void loadList(Context context){
		
		mList.clear();
		
		Cursor c = DatabaseController.retrieveDeviceList();
		
		c.moveToFirst();
		
		while (!c.isAfterLast()){
			
			Log.i("OUT","Entry: " + c.getString(1) + ";" + c.getString(2) + ";" + c.getString(3));
			
			ModelPrinter m = new ModelPrinter(c.getString(1),c.getString(2) , Integer.parseInt(c.getString(3)));
			
			addToList(m);
			m.startUpdate();
			if (m.getStatus()!=StateUtils.STATE_NEW) m.setVideoStream(context);
			
			c.moveToNext();
		}
	   
	   DatabaseController.closeDb();
	   ItemListActivity.notifyAdapters();

	}
	
	//Search first available position by listing the printers
	//TODO HARDCODED MAXIMUM CELLS
	public static int searchAvailablePosition(){
		
		int max = 12;
		boolean[] mFree = new boolean[max];
		
		for (int i = 0; i<max; i++){
			mFree[i] = false;
		
		}
		
		for (ModelPrinter p : mList){
			
			mFree[p.getPosition()] = true;
			
		}
		
		for (int i = 0; i<max; i++){
			
			if (!mFree[i]) return i;
			
		}
		
		
		return -1;
		
	}
	
	public static boolean checkExisting(ModelPrinter m){
		
		boolean exists = false;
		
		for (ModelPrinter p : mList){
			
			if ((m.getName().equals(p.getName()))||(m.getName().contains(getNetworkId(p.getName())))){
				
				exists = true;
				
			}
			
		}
		
		return exists;
		
	}
	
	//Select a printer from all the linked available  and send to print
public static void selectPrinter(final Context context, final File f){
		
		ArrayList<ModelPrinter> tempList = DevicesListController.getList();
		String[] nameList = new String[tempList.size()];
		
		//We'll check for checked items (heh) with a boolean array
		//TODO use this same method with printer discovery
		final boolean[] checkedItems = new boolean[nameList.length];
		
		int i = 0;
		
		//New array with names only for the adapter
		for (ModelPrinter p : tempList){		
			nameList[i] = p.getName();
			i++;
		}
		
		AlertDialog.Builder adb2 = new AlertDialog.Builder(context);
		adb2.setTitle(R.string.library_select_printer_title);
		
		
		
		//Show list of available printers
		//TODO Make a proper adapter
		adb2.setMultiChoiceItems(nameList, null, new OnMultiChoiceClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				
				checkedItems[which]	= isChecked;
				
			}
		});
		
		adb2.setPositiveButton(R.string.library_option_print, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
	
				//SparseBooleanArray checked = ad2.getListView().getCheckedItemPositions();;
				
				//TODO Multiprint interaction
				for (int i = 0; i<checkedItems.length ; i++){
					
					if (checkedItems[i]){
						
						ModelPrinter m = DevicesListController.getList().get(i);
						
						if (f.getParent().equals("sd")){
							OctoprintLoadAndPrint.printInternalFile(m.getAddress(), f.getName(), false, true);
			    			
						} else if (f.getParent().equals("witbox")){
							OctoprintLoadAndPrint.printInternalFile(m.getAddress(), f.getName(), false, false);
				    		
						} else {
							OctoprintLoadAndPrint.uploadFile(m.getAddress(), f, false);
						}
			    		

					}
													
				}
				
				
				
			}
		});
		
		adb2.setNegativeButton(R.string.cancel, null);
		
		adb2.show();
	}
	
	//TODO Move elsewhere maybe
	//Get the Network id key to associate with the service name
	public static String getNetworkId(String name){
			
			String[] parsedString = name.split("\\(");
			
			String parsedName = parsedString[1];
			
			return parsedName.replaceAll("[^A-Za-z0-9]", "");
			
		}
		
}
