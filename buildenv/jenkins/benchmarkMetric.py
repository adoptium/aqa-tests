import argparse, pathlib, json, re

def main():
        p = argparse.ArgumentParser()
        p.add_argument("--console", required=True)
        p.add_argument("--metricConfig_json", required=True)
        p.add_argument("--JOB_NAME", required=True)
        p.add_argument("--BUILD_NUMBER", required=True)
        args = p.parse_args()

        console = pathlib.Path(args.console).read_text(encoding="utf-8")
        metricConfig_json = pathlib.Path(args.metricConfig_json).read_text(encoding="utf-8")
        metricConfig = json.loads(metricConfig_json)
        
        metricMapList = []

        for benchmark, benchmarkChild in metricConfig.items(): 
                for (metric, metricChild) in benchmarkChild.get("metrics").items():
                        regex_parser = re.search(metricChild.get("regex"), console) 
                        if not regex_parser:
                                continue 
                        value = regex_parser.group(1)
                        higherbetter = metricChild.get("higherbetter")
                        units = metricChild.get("units")
                        metricMap = {}
                        metricMap[benchmark] = {
                                metric: {
                                        "value": value,
                                        "higher_better": higherbetter,
                                        "units": units,
                                }
                        }
                        metricMapList.append(metricMap)

        metricResults_json = json.dumps(metricMapList)
        pathlib.Path(f"{args.JOB_NAME}_{args.BUILD_NUMBER}.json").write_text(metricResults_json, encoding="utf-8")

if __name__ == "__main__":
        main()