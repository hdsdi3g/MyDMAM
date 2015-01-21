package hd3gtv.mydmam.transcode.images;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class ImageAttributes {
	
	public static class Serializer implements JsonSerializer<ImageAttributes>, JsonDeserializer<ImageAttributes> {
		@Override
		public ImageAttributes deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public JsonElement serialize(ImageAttributes src, Type typeOfSrc, JsonSerializationContext context) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
}
