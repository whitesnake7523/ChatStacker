ThelowChatToggle(TCT)説明
~使い方~
mode:fixed状態(デフォルトon)では
/p /clan msg, /tellを使用すると
自動的に次以降のチャット(/を使用するコマンドは対象外)に
先頭に/p /clan msg, /tellが付きます

mode:response状態(デフォルトoff)では
[P],[ClanChat],ささやきのメッセージを受け取った場合、
自動的に次以降のチャット(/を使用するコマンドは対象外)に
先頭に/p /clan msg, /tellが付きます

また、自動的に先頭につくコマンドが画面に表示されます(デフォルト座標 X3 Y243)
使用環境によって適切な座標の位置は変わりますが
この座標は/hudxy x y で変更することが可能です。

modeとhudxyの座標,timerはゲームを閉じても保存されます
(使用フォルダ内Configに保存)

~コマンド一覧~
/all "メッセージ"
先頭にコマンドが付いている状態で全チャをしたい場合使ってください
先頭コマンドがリセットされ全チャをすることが出来ます

/tct fix
mode:fixed状態のon offが切り替えできます

/tct res
mode:response状態のon offが切り替えできます

/tct timer
timerのon offが切り替えできます
timerをonにするとアイドル状態で90秒間経つと先頭のコマンドがリセットされます

/hudxy x座標 y座標
先頭につくコマンドが画面に表示される位置を選択できます
