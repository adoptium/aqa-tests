import argparse, pathlib, json

def initBenchmarkMetrics(metricConfig, test, benchmarkMetrics): 
        test_info = test.split("-")
        benchmarkMap = metricConfig[test_info[0]]
        metricMap = benchmarkMap["metrics"]
        if len(test_info) > 1:
                variant = test_info[1]
                if (metricMap.get(variant) != None): 
                        benchmarkMetrics.update({test : {variant : metricMap[variant]}})
                        return 

        benchmarkMetrics.update({test : metricMap})

def main():
        p = argparse.ArgumentParser()
        p.add_argument("--metricConfig_json", required=True)
        p.add_argument("--testNames", required=True)
        p.add_argument("--runBase", required=True)
        p.add_argument("--aggrBase", required=True)
        args = p.parse_args()

        metricConfig_json = pathlib.Path(args.metricConfig_json).read_text(encoding="utf-8")
        metricConfig = json.loads(metricConfig_json)
        
        benchmarkMetrics = {}
        tests = args.testNames.split(",")
        for test in tests: initBenchmarkMetrics(metricConfig, test, benchmarkMetrics)
        benchmarkMetrics_json = json.dumps(benchmarkMetrics)
        pathlib.Path(f"{args.runBase}").write_text(benchmarkMetrics_json, encoding="utf-8")

        for test in tests: 
                for metric in benchmarkMetrics[test].values():
                        metric.update({"test" : {"values" : []}})
                        metric.update({"baseline" : {"values" : []}})

        benchmarkMetrics_json = json.dumps(benchmarkMetrics)
        pathlib.Path(f"{args.aggrBase}").write_text(benchmarkMetrics_json, encoding="utf-8")

if __name__ == "__main__":
        main()