ADD_OPENS_CMD:=
ifneq ($(JDK_VERSION),8)
	ADD_OPENS_CMD=--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.util.concurrent=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.lang.invoke=ALL-UNNAMED
endif