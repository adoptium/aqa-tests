#!/usr/bin/env bash

################################################################################
# Parse Disabled Tests Script
# 
# This script replicates the GitHub workflow actions from parse-issues.yml
# Compatible with Windows (WSL), Linux, and macOS
#
# Usage:
#   export AQA_ISSUE_TRACKER_GITHUB_USER="your-username"
#   export AQA_ISSUE_TRACKER_GITHUB_TOKEN="your-token"
#   ./parse_disabled_tests.sh [optional --clean or --help]
#
# Must be run from the aqa-tests repository root directory
################################################################################

set -e  # Exit on error
set -u  # Exit on undefined variable
set -o pipefail # Exit on pipe failure

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Functions to print messages
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_group() {
    echo -e "\n${GREEN}::group::${NC}${BLUE}$1${NC}"
}

log_endgroup() {
    echo -e "${GREEN}::endgroup::${NC}\n"
}

usage() {
    cat << EOF
Usage: $(basename "$0") [OPTIONS]

Parse disabled tests from AQA test repository.

OPTIONS:
    -c, --clean     Clean output directory before running
    -h, --help      Show this help message

ENVIRONMENT VARIABLES:
    AQA_ISSUE_TRACKER_GITHUB_USER   GitHub username for API access
    AQA_ISSUE_TRACKER_GITHUB_TOKEN  GitHub token for API access

EXAMPLES:
    $(basename "$0")              # Run with existing output
    $(basename "$0") -c           # Clean and run

Must be run from the aqa-tests repository root directory.
EOF
    exit 0
}

# Parse command line arguments
CLEAN_OUTPUT=false
VERBOSE=false
OUTPUT_DIR="./output"

while [[ $# -gt 0 ]]; do
    case "$1" in
        -c|--clean)
            CLEAN_OUTPUT=true
            shift
            ;;
        -h|--help)
            usage
            ;;
        *)
            log_error "Unknown option: $1"
            echo "Use -h or --help for usage information"
            exit 1
            ;;
    esac
done

# Output directory preparations
if [[ -d "$OUTPUT_DIR" ]]; then
    if [[ "$CLEAN_OUTPUT" == "true" ]]; then
        log_info "Clearing preexisting output directory..."
        rm -rf "${OUTPUT_DIR}"
        log_info "Output directory cleared."
    else
        log_warning "Output directory already exists. Use -c to clear and re-run."
        exit 1
    fi
fi

log_info "Creating output directory..."
mkdir "$OUTPUT_DIR" || {
    log_error "Output directory could not be created: ${OUTPUT_DIR}"
    exit 1
}
log_info "Output directory created."

# Output file names
EXCLUDE_FILES="${OUTPUT_DIR}/exclude_files.txt"
PLAYLIST_FILES="${OUTPUT_DIR}/playlist_files.txt"
EXCLUDE_JSON="${OUTPUT_DIR}/exclude.json"
PLAYLIST_JSON="${OUTPUT_DIR}/playlist.json"
ALL_JSON="${OUTPUT_DIR}/all.json"
OUTPUT_JSON="${OUTPUT_DIR}/output.json"

# Validate JSON file contents
validate_json_file() {
    local file="$1"
    local description="$2"
    
    if [[ ! -f "$file" ]]; then
        log_error "$description file not created: $file"
        return 1
    fi
    
    if ! jq empty "$file" 2>/dev/null; then
        log_error "$description contains invalid JSON: $file"
        return 1
    fi
    
    local count
    count=$(jq 'length' "$file" 2>/dev/null || echo "0")
    log_info "$description contains $count entries"
    return 0
}

################################################################################
# Pre-flight checks
################################################################################

log_info "Starting parse disabled tests script..."

# Check if we're in the correct directory
if [[ ! -d "openjdk/excludes" ]] || [[ ! -d "scripts/disabled_tests" ]]; then
    log_error "This script must be run from the aqa-tests repository root directory"
    log_error "Current directory: $(pwd)"
    exit 1
fi

# Check for required commands
REQUIRED_COMMANDS=("python3" "pip3" "jq")
for cmd in "${REQUIRED_COMMANDS[@]}"; do
    if ! command -v "$cmd" &> /dev/null; then
        log_error "Required command '$cmd' not found. Please install it first."
        exit 1
    fi
done

# Check Python version (need 3.8+)
PYTHON_VERSION=$(python3 --version | awk '{print $2}')
PYTHON_MAJOR=$(echo "$PYTHON_VERSION" | cut -d. -f1)
PYTHON_MINOR=$(echo "$PYTHON_VERSION" | cut -d. -f2)

if [[ "$PYTHON_MAJOR" -lt 3 ]] || [[ "$PYTHON_MAJOR" -eq 3 && "$PYTHON_MINOR" -lt 8 ]]; then
    log_error "Python 3.8 or higher is required. Found: $PYTHON_VERSION"
    exit 1
fi

log_success "Python version $PYTHON_VERSION detected"

# Check for GitHub credentials (optional but recommended)
if [[ -z "${AQA_ISSUE_TRACKER_GITHUB_USER:-}" ]] || [[ -z "${AQA_ISSUE_TRACKER_GITHUB_TOKEN:-}" ]]; then
    log_warning "GitHub credentials not set. Issue status checking may be limited."
    log_warning "Set AQA_ISSUE_TRACKER_GITHUB_USER and AQA_ISSUE_TRACKER_GITHUB_TOKEN environment variables."
else
    log_success "GitHub credentials detected"
fi

################################################################################
# Step 1: Install Python requirements
################################################################################

log_info "Checking if Python requirements are already installed."
while read rawRequirement; do
    singleRequirement="$(echo "$rawRequirement" | tr -d '[:space:]' | tr -d '\r\n')"
    if ! pip3 show "$singleRequirement" &> /dev/null; then
        log_error "One or more of the Python requirements were not found."
        log_info "Use this command to install the requirements before rerunning this script."
        log_info "pip3 install -q -r scripts/disabled_tests/requirements.txt"
        exit 1
    fi
done < scripts/disabled_tests/requirements.txt

log_success "Python requirements already installed"

################################################################################
# Step 2: Discover disabled tests
################################################################################

log_group "openjdk exclude files"
find openjdk/excludes -name '*ProblemList*.txt' | tee "${EXCLUDE_FILES}"
log_endgroup

log_group "playlist files"
find . -name "playlist.xml" -not -path "*/scripts/*" | tee "${PLAYLIST_FILES}"
log_endgroup

EXCLUDE_COUNT=$(wc -l < "${EXCLUDE_FILES}")
PLAYLIST_COUNT=$(wc -l < "${PLAYLIST_FILES}")
log_success "Found $EXCLUDE_COUNT exclude files and $PLAYLIST_COUNT playlist files"

################################################################################
# Step 3: Run parsing scripts
################################################################################

log_group "parsing"

log_info "Parsing exclude files..."
cat "${EXCLUDE_FILES}" | python3 scripts/disabled_tests/exclude_parser.py -v > "${EXCLUDE_JSON}"
validate_json_file "${EXCLUDE_JSON}" "Exclude data" || exit 1
log_success "Exclude files parsed"

log_info "Parsing playlist files..."
cat "${PLAYLIST_FILES}" | python3 scripts/disabled_tests/playlist_parser.py -v > "${PLAYLIST_JSON}"
validate_json_file "${PLAYLIST_JSON}" "Playlist data" || exit 1
log_success "Playlist files parsed"

log_endgroup

################################################################################
# Step 4: Merge results
################################################################################

log_group "merging"
log_info "Merging exclude and playlist data..."
jq -s 'flatten(1)' "${EXCLUDE_JSON}" "${PLAYLIST_JSON}" > "${ALL_JSON}"
validate_json_file "${ALL_JSON}" "Merged data" || exit 1
MERGED_COUNT=$(jq 'length' "${ALL_JSON}")
log_success "Merged $MERGED_COUNT disabled test entries"
log_endgroup

################################################################################
# Step 5: Check issue status
################################################################################

log_group "status"
log_info "Checking issue status..."
cat "${ALL_JSON}" | python3 scripts/disabled_tests/issue_status.py -v > "${OUTPUT_JSON}"
validate_json_file "${OUTPUT_JSON}" "Output data" || exit 1
OUTPUT_COUNT=$(jq 'length' "${OUTPUT_JSON}")
log_success "Generated output with $OUTPUT_COUNT entries"
log_endgroup

################################################################################
# Summary
################################################################################

log_success "Script completed successfully!"
echo ""
log_info "Generated files:"
echo "  - ${EXCLUDE_FILES}  : List of exclude files"
echo "  - ${PLAYLIST_FILES} : List of playlist files"
echo "  - ${EXCLUDE_JSON}   : Parsed exclude data"
echo "  - ${PLAYLIST_JSON}  : Parsed playlist data"
echo "  - ${ALL_JSON}       : Merged data"
echo "  - ${OUTPUT_JSON}    : Final output with issue status"
echo ""
log_info "Main output file: ${OUTPUT_JSON}"

exit 0
