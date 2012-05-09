package io.leocad.dumbledroid.data;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class ModelReflector {

	public static void reflectJson(Object model, String contentString) throws JSONException {

		JSONObject jsonObj = null;
		
		try {
			jsonObj = new JSONObject(contentString);
		
		} catch (JSONException e) {
			//This is a JSONArray
		}

		if (jsonObj != null) {
			reflectJsonObject(model, jsonObj);
		} else {
			reflectJsonArray(model, new JSONArray(contentString));
		}
		
	}
	
	private static void reflectJsonObject(Object model, JSONObject jsonObj) throws JSONException {
		Class<?> modelClass = model.getClass();
		JSONArray names = jsonObj.names();

		for (int i = 0; i < names.length(); i++) {

			String name = names.getString(i);

			try {
				Field field = modelClass.getField(name);

				if (field.getType() == List.class) {
					processListField(model, field, jsonObj.getJSONArray(name));

				} else {
					processSingleField(model, field, jsonObj, name);
				}

			} catch (NoSuchFieldException e) {
				Log.w(ModelReflector.class.getName(), "Can not locate field named " + name);

			} catch (IllegalArgumentException e) {
				Log.w(ModelReflector.class.getName(), "Can not put a String in the field named " + name);

			} catch (IllegalAccessException e) {
				Log.w(ModelReflector.class.getName(), "Can not access field named " + name);
			
			} catch (InstantiationException e) {
				Log.w(ModelReflector.class.getName(), "Can not create an instance of the type defined in the field named " + name);
			}
		}
	}
	
	private static void reflectJsonArray(Object model, JSONArray jsonArray) throws JSONException {
		
		Class<?> modelClass = model.getClass();
		try {
			Field listField = modelClass.getField("list");
			processListField(model, listField, jsonArray);
			
		} catch (NoSuchFieldException e) {
			Log.w(ModelReflector.class.getName(), "Can not locate field named list");
			
		} catch (IllegalArgumentException e) {
			Log.w(ModelReflector.class.getName(), "Can not put a List in the field named list");
			
		} catch (IllegalAccessException e) {
			Log.w(ModelReflector.class.getName(), "Can not access field named list");
			
		} catch (InstantiationException e) {
			Log.w(ModelReflector.class.getName(), "Can not create an instance of the type defined in the field named list");
		}
	}

	private static void processSingleField(Object model, Field field, JSONObject jsonObj, String nodeName)
			throws JSONException, IllegalArgumentException, IllegalAccessException, InstantiationException {

		Class<?> type = field.getType();

		field.set(model, getObject(jsonObj, type, nodeName));
	}

	private static Object getObject(JSONObject jsonObj, Class<?> type, String nodeName) throws JSONException, InstantiationException {

		if (type == String.class) {
			return jsonObj.getString(nodeName);

		} else if (type == boolean.class || type == Boolean.class) {
			return jsonObj.getBoolean(nodeName);

		} else if (type == int.class || type == Integer.class) {
			return jsonObj.getInt(nodeName);

		} else if (type == double.class || type == Double.class) {
			return jsonObj.getDouble(nodeName);

		} else {
			Object obj;
			try {
				obj = type.newInstance();
				
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				return null;
			}
			
			reflectJsonObject(obj, jsonObj.getJSONObject(nodeName));
			
			return obj;
		}
	}

	private static void processListField(Object object, Field field, JSONArray jsonArray)
			throws JSONException, IllegalArgumentException, IllegalAccessException, InstantiationException {

		ParameterizedType genericType = (ParameterizedType) field.getGenericType();
		Class<?> childrenType = (Class<?>) genericType.getActualTypeArguments()[0];

		field.set(object, getList(jsonArray, childrenType));
	}

	private static List<?> getList(JSONArray jsonArray, Class<?> childrenType) throws JSONException, InstantiationException, IllegalAccessException {

		List<Object> list = new Vector<Object>(jsonArray.length());

		for (int i = 0; i < jsonArray.length(); i++) {

			Object child = null;

			if (childrenType == List.class) {
				child = getList(jsonArray.getJSONArray(i), childrenType);

			} else if (childrenType == String.class) {
				child = jsonArray.getString(i);

			} else if (childrenType == boolean.class || childrenType == Boolean.class) {
				child = jsonArray.getBoolean(i);

			} else if (childrenType == int.class || childrenType == Integer.class) {
				child = jsonArray.getInt(i);

			} else if (childrenType == double.class || childrenType == Double.class) {
				child = jsonArray.getDouble(i);

			} else {
				child = childrenType.newInstance();
				reflectJsonObject(child, jsonArray.getJSONObject(i));
			}

			list.add(child);
		}

		return list;
	}
}
