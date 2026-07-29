import argparse, pathlib, json, re

#regex used to convert BenchmarkMetric.js into valid JSON file
RE_COMMENT = re.compile(r"""
        //.*?$ |
        /\*.*?\*/
        """, re.DOTALL | re.MULTILINE | re.VERBOSE)

RE_TRAIL_COMMA = re.compile(r",(\s*[}\]])")

RE_REGEX = re.compile(r"""/((?:\\.|[^/\\])*?)/[gimsuy]*(?=\s*,)""")

RE_FUNC = re.compile(r"""(funcName:\s)(.*?)(,)""")

RE_KEYS = re.compile(r"""([,{]\s*)([A-Za-z_]\w*)(\s*:)""")

#parses the BenchmarkMetric.js file by grabbing the BenchmarkMetricRegex element, 
#removing comments, and converting to proper JSON syntax 
def js_to_json(metrics_js):
        benchmark_parser = re.search(r"const\s+BenchmarkMetricRegex\s*=\s*({[\s\S]*?});", metrics_js) 
        if not benchmark_parser:
                raise ValueError("BenchmarkMetricRegex not found")
        obj = benchmark_parser.group(1)
        obj = obj.replace("'", '"') #convert units and string keys 
        obj = RE_COMMENT.sub("", obj) #remove comments 
        obj = RE_REGEX.sub(lambda m: json.dumps(m.group(1)), obj) #convert regex 
        obj = RE_FUNC.sub(r'\1"\2"\3', obj) #convert funcName
        obj = RE_TRAIL_COMMA.sub(r'\1', obj) #remove trailing commas after funcName and regex conversion
        obj = RE_KEYS.sub(r'\1"\2"\3', obj) #convert non string keys after removing trailing commas
        return obj

def main():
        p = argparse.ArgumentParser()
        p.add_argument("--metricConfig_js", required=True)
        args = p.parse_args()
        metricConfig_js = pathlib.Path(args.metricConfig_js).read_text(encoding="utf-8")
        metricConfig_json = js_to_json(metricConfig_js)
        pathlib.Path("metricConfig.json").write_text(metricConfig_json, encoding="utf-8")

if __name__ == "__main__":
        main()