ATOM Processing

== Audio seul ==

ffmpeg -y -i ATOM0 -vn -codec:a pcm_s16le -map "0:0" -f wav out0.wav
ffmpeg -y -i ATOMn -vn -codec:a pcm_s16le -map "0:n" -f wav outn.wav
raw2bmx -t op1a -y <TC> --clip <NAME> -o OUT.mxf --wave out0.wav --wave outn.wav

== Video + audio par Vn / MC ==

mxf2raw --ess-out V0 ATOM_V.mxf
ffmpeg -y -i ATOM1 -vn -codec:a pcm_s16le -map "0:1" -f wav out1.wav
ffmpeg -y -i ATOMn -vn -codec:a pcm_s16le -map "0:n" -f wav outn.wav
raw2bmx -t op1a -y <TC> --clip <NAME> -o OUT.mxf --mpeg2lg_422p_hl_1080i "V0_v0.raw" --wave out1.wav --wave outn.wav

== Image fixe MC ==
mxf2raw --ess-out ATOM0 ATOM_V.mxf
raw2bmx -t op1a -y <TC> --clip <NAME> -o OUT.mxf --mpeg2lg_422p_hl_1080i "ATOM0_v0.raw"

== Video + audio par MC ==

Attention a bien recup les map audio de ffmpeg : Vn != MC
