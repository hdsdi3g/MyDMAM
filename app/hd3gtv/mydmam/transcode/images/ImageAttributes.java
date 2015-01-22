package hd3gtv.mydmam.transcode.images;

import hd3gtv.mydmam.metadata.container.Entry;
import hd3gtv.mydmam.metadata.container.EntryAnalyser;
import hd3gtv.mydmam.metadata.container.SelfSerializing;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class ImageAttributes extends EntryAnalyser {
	
	// TODO @see FFprobe
	
	@SerializedName("Rendering intent")
	String rendering_intent;
	
	@Override
	protected void extendedInternalSerializer(JsonObject current_element, EntryAnalyser _item, Gson gson) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void extendedInternalDeserialize(EntryAnalyser _item, JsonObject source, Gson gson) {
		// TODO Auto-generated method stub
		
	}
	
	public String getES_Type() {
		return "identify";
	}
	
	protected Entry create() {
		return new ImageAttributes();
	}
	
	@Override
	protected List<Class<? extends SelfSerializing>> getSerializationDependencies() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
