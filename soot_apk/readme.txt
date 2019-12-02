
# best to run ./make_app 


# compile the java instrument driver:
javac -cp sootclasses-trunk-jar-with-dependencies.jar AndroidInstrument.java

# run on a .apk
java -cp sootclasses-trunk-jar-with-dependencies.jar:. AndroidInstrument -android-jars ./ -process-dir ./nRF_UART.apk

# to extract cfg for all phases (but we only use jtp here) use:
java -cp sootclasses-trunk-jar-with-dependencies.jar:. AndroidInstrument -android-jars ./ -process-dir ./nRF_UART.apk --dump-cfg ALL

# this places .dot files for each method in the output folder, viewable with xdot in linux

# to convert a .dot graph another format use:

dot -Tpng input_file.dot > filename.png
dot -Tjson input_file.dot

# for other files (e.g. chrome..), they have other dependencies, so we do this 
java -cp sootclasses-trunk-jar-with-dependencies.jar:./sun/misc/Unsafe.class:. AndroidInstrument -allow-phantom-refs -android-jars ./ -process-dir ./Chrome_com.android.chrome.apk

# in order to sign a .apk so you can install it (changing names accordingly)

keytool -genkey -v -keystore my-release-key.keystore -alias alias_name -keyalg RSA -keysize 2048 -validity 10000

jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore my-release-key.keystore my_application.apk alias_name

# in order to view printlns that were inserted into the app, use (with the appropriate device selected, if more than one present):

adb logcat
