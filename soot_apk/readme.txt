
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