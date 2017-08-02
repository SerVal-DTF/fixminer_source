package edu.lu.uni.serval.FixPatternMining;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.lu.uni.serval.FixPatternMining.DataPrepare.DataPreparation;
import edu.lu.uni.serval.utils.MapSorter;

public class CommonPatterns {
	
	private static final int LEAST_NUMBER = 100;
	private int totalNumberofTrainingData = 0;

	public Map<Integer, Integer> identifyCommonPatterns(List<Integer> clusterResults) {
		Map<Integer, List<Integer>> clusterMap = DataPreparation.readClusterResult(clusterResults);
		// TODO how to select the common patterns, number or ratio?
		List<Integer> commonClusterNum = getCommonClustersByNumber(clusterMap); // Integer: clusterNum.
		
		Map<Integer, Integer> clusterNumMapLabel = new HashMap<>(); // <ClusterNum, Label for supervised learning>
		for (int i = 0, size = commonClusterNum.size(); i < size; i ++) {
			clusterNumMapLabel.put(commonClusterNum.get(i), i);
		}
		
		return clusterNumMapLabel;
	}
	
	private List<Integer> getCommonClustersByNumber(Map<Integer, List<Integer>> clusterMap) {
		List<Integer> commonClusterNum = new ArrayList<>();
		
		for (Map.Entry<Integer, List<Integer>> entry : clusterMap.entrySet()) {
			List<Integer> elements = entry.getValue();
			int size = elements.size();
			if (size >= LEAST_NUMBER) { // TODO how to set this threshold?
				commonClusterNum.add(entry.getKey());
				totalNumberofTrainingData += size;
			}
		}
		
		return commonClusterNum;
	}
	
	private List<Integer> getCommonClustersByRatio(Map<Integer, List<Integer>> clusterMap, List<Integer> clusterResults) {
		List<Integer> commonClusterNum = new ArrayList<>();
		
		double sizes = (double) clusterResults.size();
		Map<Integer, Double> ratios = new HashMap<>();
		for (Map.Entry<Integer, List<Integer>> entry : clusterMap.entrySet()) {
			List<Integer> elements = entry.getValue();
			ratios.put(entry.getKey(), (double) elements.size() / sizes);
		}
		MapSorter<Integer, Double> sorter = new MapSorter<Integer, Double>();
		ratios = sorter.sortByValueDescending(ratios);
		double counterRatio = 0.0;
		for (Map.Entry<Integer, Double> entry : ratios.entrySet()) {
			counterRatio += entry.getValue();
			commonClusterNum.add(entry.getKey());
			totalNumberofTrainingData += clusterMap.get(entry.getKey()).size();
			if (counterRatio >= 0.8) { // TODO: how to set the value of this threshold?
				break;
			}
		}
		
		return commonClusterNum;
	}

	public int getTotalNumberofTrainingData() {
		return totalNumberofTrainingData;
	}
	
}
