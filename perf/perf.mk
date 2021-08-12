ADD_OPENS_CMD=""
ifneq ($(JDK_VERSION),8)
	ADD_OPENS_CMD=$(Q)--add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.util.concurrent=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED$(Q)
endif