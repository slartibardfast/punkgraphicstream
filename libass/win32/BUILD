Dependencies can be built or cross-compiled with a working mingw32 setup

Native Library as required by jar can be built with this (darwin port's mingw32 & a win32 jdk):
i386-mingw32-gcc -Wall -D_JNI_IMPLEMENTATION_ -Wl,--kill-at -I$JAVA_HOME/include -I$JAVA_HOME/include/win32 -Iwin32/include -Iwin32/include/freetype2 -Iwin32/include/libxml2 -Lwin32/lib -shared -o win32/bin/ass.dll *.c -lfreetype -lexpat -lfontconfig

Native Library can be build with this (cygwin's mingw32):
gcc -mno-cygwin -Wall -D_JNI_IMPLEMENTATION_ -Wl,--kill-at -I$JAVA_HOME/include -I$JAVA_HOME/include/win32 -Iwin32/include -Iwin32/include/freetype2 -Iwin32/include/libxml2 -Lwin32/lib -shared -o win32/bin/ass.dll *.c -lfreetype -lexpat -lfontconfig

[TODO] Proper build system!
