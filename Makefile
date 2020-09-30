# |--------------------------------------------------------------------------
# | Variables definitions
# |--------------------------------------------------------------------------

# ANSI colors escape sequence variables
COLOR_NORMAL := "\033[0m"
COLOR_COMMENT := "\033[1;33m"

# Makefile special variables
.DEFAULT_GOAL := help

# |--------------------------------------------------------------------------
# | Targets definitions
# |--------------------------------------------------------------------------

.PHONY: help
help: ## Prints this help
	@grep -E '^[a-zA-Z\/_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

.PHONY: build
build: ## Build all sub-projects
	@echo "🔨 Building all sub-projects"
	./gradlew build

.PHONY: deploy
clean: ## Clean all sub-projects
	@echo "🧹 Cleaning all sub-projects"
	./gradlew clean

.PHONY: deploy
deploy:
	@echo "🚀 Starting deployment"
	./scripts/deploy.sh

.PHONY: addlicense
addlicense:
	@echo "📝 Adding license at the top of every source code file"
	docker run -e OPTIONS="-s -c infinitic.io -f .license-header -config .addlicense.yml" --rm -it -v $(shell pwd):/myapp nokia/addlicense-nokia:latest
