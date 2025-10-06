.PHONY: build build-IngestFunction clean

build:
	@echo "Building with Maven from project root: $(PWD)"
	cd $(PWD) && mvn -DskipTests clean package

build-IngestFunction: build
	@echo "Copying built JAR to SAM artifacts dir..."
	mkdir -p $(ARTIFACTS_DIR)
	cp $(shell find $(PWD)/target -maxdepth 1 -type f -name "*.jar" ! -name "original-*" | head -1) $(ARTIFACTS_DIR)/function.jar

clean:
	rm -rf target .aws-sam
