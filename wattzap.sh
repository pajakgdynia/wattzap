LIBS="lib/wattzap.jar:properties:lib/log4j-1.2.17.jar:lib/derby.jar:lib/opencsv-2.3.jar:lib/com-sun-tools-visualvm-charts.jar:lib/com-sun-tools-visualvm-uisupport.jar:lib/gpx-creator-0.1-beta.jar:lib/hamcrest-core-1.3.jar:lib/jcommon-1.0.17.jar:lib/jcommon-1.0.18.jar:lib/jformica_core.jar:lib/jformica_jsr80.jar:lib/jfreechart-1.0.14.jar:lib/jna-3.5.1.jar:lib/junit-4.11.jar:lib/miglayout-core-4.2.jar:lib/miglayout-swing-4.2.jar:lib/org-netbeans-lib-profiler-charts.jar:lib/org-netbeans-lib-profiler-ui.jar:lib/org-openide-util-lookup.jar:lib/org-openide-util.jar:lib/usb-api-1.0.2.jar:lib/usb4java-1.2.0.jar:lib/vlcj-2.2.0.jar:lib/usb4java-javax-1.2.0.jar:lib/commons-lang3-3.2.1.jar "

#java -cp $LIBS -Dlog4j.logger.level=DEBUG com.wattzap.Main
java -cp $LIBS com.wattzap.Main
