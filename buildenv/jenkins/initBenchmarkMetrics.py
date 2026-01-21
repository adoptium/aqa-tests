import argparse, pathlib, json

#extract necessary benchmark information from metricConfig based on test, and 
#update benchmarkMetrics such that it is optimal for later processing 
def initBenchmarkMetrics(metricConfig, test, benchmarkMetrics): 
        test_info = test.split("-")
        benchmarkMap = metricConfig[test_info[0]] #index by general test category
        metricMap = benchmarkMap["metrics"]
        if len(test_info) > 1: #if there is a variant, grab it directly 
                variant = test_info[1]
                if (metricMap.get(variant) != None): 
                        benchmarkMetrics.update({test : {variant : metricMap[variant]}})
                        return 

        #if there is no variant, we take the metricMap to already contain the unique information needed for test
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
        pathlib.Path(f"{args.runBase}").write_text(benchmarkMetrics_json, encoding="utf-8") #serves as template populated by a single run

        for test in tests: 
                for metric in benchmarkMetrics[test].values():
                        metric.update({"test" : {"values" : []}})
                        metric.update({"baseline" : {"values" : []}})

        benchmarkMetrics_json = json.dumps(benchmarkMetrics)
        pathlib.Path(f"{args.aggrBase}").write_text(benchmarkMetrics_json, encoding="utf-8") #serves as aggregate file populated by all runs

if __name__ == "__main__":
        main()