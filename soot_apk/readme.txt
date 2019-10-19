
# compile the java instrument driver:
javac -cp sootclasses-trunk-jar-with-dependencies.jar AndroidInstrument.java

# run on a .apk
java -cp sootclasses-trunk-jar-with-dependencies.jar:. AndroidInstrument -android-jars ./ -process-dir ./nRF_UART.apk