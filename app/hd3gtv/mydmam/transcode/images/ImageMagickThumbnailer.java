package hd3gtv.mydmam.transcode.images;

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.metadata.GeneratorRenderer;
import hd3gtv.mydmam.metadata.PreviewType;
import hd3gtv.mydmam.metadata.RenderedFile;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.mydmam.metadata.container.SelfSerializing;
import hd3gtv.mydmam.module.MyDMAMModulesManager;
import hd3gtv.mydmam.transcode.TranscodeProfile;
import hd3gtv.mydmam.transcode.TranscodeProfile.ProcessConfiguration;
import hd3gtv.tools.ExecprocessGettext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ImageMagickThumbnailer implements GeneratorRenderer {
	
	// TODO limits : -limit memory 100MB -limit map 100MB -limit area 100MB -limit disk 30MB -limit file 50 -limit time 50
	
	public static class FullDisplay extends EntryRenderer {
		public String getES_Type() {
			return "imthumbnail_full";
		}
		
		protected EntryRenderer create() {
			return new FullDisplay();
		}
		
		protected List<Class<? extends SelfSerializing>> getSerializationDependencies() {
			return null;
		}
		
		public static final String profile_name = "convert_full_display";
	}
	
	public static class Cartridge extends EntryRenderer {
		public String getES_Type() {
			return "imthumbnail_cartridge";
		}
		
		protected EntryRenderer create() {
			return new Cartridge();
		}
		
		protected List<Class<? extends SelfSerializing>> getSerializationDependencies() {
			return null;
		}
		
		public static final String profile_name = "convert_cartridge";
	}
	
	public static class Icon extends EntryRenderer {
		public String getES_Type() {
			return "imthumbnail_icon";
		}
		
		protected EntryRenderer create() {
			return new Icon();
		}
		
		protected List<Class<? extends SelfSerializing>> getSerializationDependencies() {
			return null;
		}
		
		public static final String profile_name = "convert_icon";
	}
	
	private String convert_bin;
	private TranscodeProfile tprofile_opaque;
	private TranscodeProfile tprofile_alpha;
	private Class<? extends EntryRenderer> root_entry_class;
	private PreviewType preview_type;
	private static File icc_profile;
	
	static {
		LinkedHashMap<String, File> conf_dirs = MyDMAMModulesManager.getAllConfDirectories();
		File conf_dir;
		FilenameFilter fnf = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.equals("srgb.icc");
			}
		};
		for (Map.Entry<String, File> entry : conf_dirs.entrySet()) {
			conf_dir = entry.getValue();
			File[] modules_files = conf_dir.listFiles(fnf);
			for (int pos_mf = 0; pos_mf < modules_files.length; pos_mf++) {
				icc_profile = modules_files[pos_mf];
			}
		}
		
		if (icc_profile == null) {
			Log2.log.error("Can't found icc profile file in conf directory.", new FileNotFoundException("conf/srgb.icc"));
		}
	}
	
	public ImageMagickThumbnailer(Class<? extends EntryRenderer> root_entry_class, PreviewType preview_type, String profile_name) {
		this.root_entry_class = root_entry_class;
		if (root_entry_class == null) {
			throw new NullPointerException("\"root_entry_class\" can't to be null");
		}
		this.preview_type = preview_type;
		if (preview_type == null) {
			throw new NullPointerException("\"preview_type\" can't to be null");
		}
		convert_bin = Configuration.global.getValue("transcoding", "convert_bin", "convert");
		
		if (TranscodeProfile.isConfigured()) {
			tprofile_opaque = TranscodeProfile.getTranscodeProfile(profile_name);
			tprofile_alpha = TranscodeProfile.getTranscodeProfile(profile_name + "_alpha");
		}
	}
	
	public boolean canProcessThis(String mimetype) {
		return ImageMagickAnalyser.mimetype_list.contains(mimetype);
	}
	
	public boolean isEnabled() {
		return (new File(convert_bin)).exists() & (tprofile_opaque != null);
	}
	
	public String getLongName() {
		return "ImageMagick low-res image thumbnailer";
	}
	
	public PreviewType getPreviewTypeForRenderer(Container container, EntryRenderer entry) {
		return preview_type;
	}
	
	public Class<? extends EntryRenderer> getRootEntryClass() {
		return root_entry_class;
	}
	
	public EntryRenderer process(Container container) throws Exception {
		ImageAttributes image_attributes = container.getByClass(ImageAttributes.class);
		if (image_attributes == null) {
			Log2.log.debug("No image_attributes for this container", container);
			return null;
		}
		
		TranscodeProfile tprofile = tprofile_opaque;
		if (image_attributes.alpha != null) {
			tprofile = tprofile_alpha;
		}
		
		if (root_entry_class != Icon.class) {
			/**
			 * Icon image will be allways created.
			 */
			if ((tprofile.getOutputformat().getResolution().x > image_attributes.geometry.width) & (tprofile.getOutputformat().getResolution().y > image_attributes.geometry.height)) {
				Log2Dump dump = new Log2Dump();
				dump.add("Source", container);
				dump.add("output format", tprofile.getOutputformat());
				Log2.log.debug("Image size is too litte to fit in this profile", dump);
				return null;
			}
		}
		
		RenderedFile element = new RenderedFile(root_entry_class.getSimpleName().toLowerCase(), tprofile.getExtension("jpg"));
		ProcessConfiguration process_conf = tprofile.createProcessConfiguration(convert_bin, container.getPhysicalSource(), element.getTempFile());
		process_conf.getParamTags().put("ICCPROFILE", icc_profile.getAbsolutePath());
		
		ExecprocessGettext process = process_conf.prepareExecprocess();
		Log2.log.debug("Start conversion", process_conf);
		process.start();
		
		return element.consolidateAndExportToEntry(root_entry_class.newInstance(), container, this);
	}
}
