(function(a){a.Audio=React.createClass({displayName:"Audio",getInitialState:function(){return{currentTime:null,duration:null,transport:null};
},transportStatusChange:function(c,d,b){this.setState({currentTime:c,duration:d,transport:null});
},goToNewTime:function(b){this.setState({transport:{gototime:b}});},render:function(){var e=this.props.file_hash;
var f=this.props.mtdsummary.previews;var c=this.props.mtdsummary.mimetype;var b=this.props.reference;
var g=this.props.master_as_preview_url;var d=null;if(g){d=g;}else{if(f){d=a.getFileURL(e,f.audio_pvw.type,f.audio_pvw.file);
}}if(d==null){return null;}var h=null;if(a.hasAudioGraphicDeepAnalyst(f)){h=this.transportStatusChange;
}return(React.createElement("div",{style:{marginBottom:"1em"}},React.createElement(a.Mediaplayer,{transport:this.state.transport,transport_status:h,audio_only:true,cantloadingplayerexcuse:i18n("browser.cantloadingplayer"),source_url:d}),React.createElement(a.AudioGraphicDeepAnalyst,{previews:f,file_hash:e,currentTime:this.state.currentTime,duration:this.state.duration,goToNewTime:this.goToNewTime}),React.createElement(a.Image,{file_hash:e,previews:f,hide_audio_da_grahic:true,prefered_size:"cartridge_thumbnail"})));
}});})(window.mydmam.async.pathindex.metadatas);