mkdir -p classes
javac -d classes src/adaptorlib/*java src/adaptorlib/tests/*java src/templateadaptor/*java
copy src/logging.properties classes/

REM TODO: Reintroduce src/local-fs-adaptor/*java .