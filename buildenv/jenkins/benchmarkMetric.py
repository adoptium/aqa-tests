import argparse, pathlib, json, re

def main():
        p = argparse.ArgumentParser()
        p.add_argument("--console", required=True)
        p.add_argument("--benchmarkMetricsTemplate_json", required=True)
        p.add_argument("--fname", required=True)
        p.add_argument("--testNames", required=True)
        args = p.parse_args()

        console = pathlib.Path(args.console).read_text(encoding="utf-8")

        benchmarkMetricsTemplate_json = pathlib.Path(args.benchmarkMetricsTemplate_json).read_text(encoding="utf-8")
        benchmarkMetricsTemplate = json.loads(benchmarkMetricsTemplate_json)

        tests = args.testNames.split(",") 
        
        #populate the template file with corresponding metrics extracted from console log
        for test in tests: 
                for metric in benchmarkMetricsTemplate[test].values():
                        regex_parser = re.search(metric.get("regex"), console)
                        if not regex_parser: continue 
                        metric.update({"value" : float(regex_parser.group(1))})

        benchmarkMetricsTemplate_json = json.dumps(benchmarkMetricsTemplate)
        pathlib.Path(f"{args.fname}").write_text(benchmarkMetricsTemplate_json, encoding="utf-8")

if __name__ == "__main__":
        main()