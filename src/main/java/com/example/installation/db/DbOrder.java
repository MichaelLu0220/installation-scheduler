package com.example.installation.db;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ArrayList;

public class DbOrder {
	private Long id;
	private String machineName;
	private LocalDate dueDate;
	private LocalDate etaDate;
	private String strategy;
	private String status;
	private List<MaterialRequirement> materials = new ArrayList<>();

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getMachineName() {
		return machineName;
	}

	public void setMachineName(String machineName) {
		this.machineName = machineName;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public LocalDate getEtaDate() {
		return etaDate;
	}

	public void setEtaDate(LocalDate etaDate) {
		this.etaDate = etaDate;
	}

	public String getStrategy() {
		return strategy;
	}

	public void setStrategy(String strategy) {
		this.strategy = strategy;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public List<MaterialRequirement> getMaterials() {
		return materials;
	}

	public void setMaterials(List<MaterialRequirement> materials) {
		this.materials = materials;
	}

	// ✅ 新增：動態計算優先級
	public String getPriority() {
		if (dueDate == null)
			return "Normal";

		long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);

		if (daysUntilDue <= 7)
			return "High";
		if (daysUntilDue <= 30)
			return "Medium";
		return "Normal";
	}

	// ✅ 新增：檢查是否有材料風險
	public boolean hasMaterialRisk() {
		if (materials == null || materials.isEmpty())
			return false;
		return materials.stream().anyMatch(m -> m.getShortage() > 0);
	}

	// ✅ 新增：計算材料滿足率
	public double getMaterialSatisfactionRate() {
		if (materials == null || materials.isEmpty())
			return 100.0;

		double totalNeeded = materials.stream().mapToDouble(MaterialRequirement::getQtyNeeded).sum();
		double totalSatisfied = materials.stream().mapToDouble(m -> Math.min(m.getQtyNeeded(), m.getQtyOnHand())).sum();

		return totalNeeded > 0 ? (totalSatisfied / totalNeeded) * 100 : 100.0;
	}

	// ✅ 新增：格式化的工單ID
	public String getFormattedJobId() {
		return "工單-" + (id != null ? id : "N/A");
	}

	// ✅ 新增：計算剩餘天數
	public long getDaysUntilDue() {
		if (dueDate == null)
			return Long.MAX_VALUE;
		return ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
	}

	// ✅ 新增：檢查是否逾期
	public boolean isOverdue() {
		return getDaysUntilDue() < 0;
	}

	// ✅ 新增：檢查是否即將到期（7天內）
	public boolean isDueSoon() {
		long days = getDaysUntilDue();
		return days >= 0 && days <= 7;
	}

	// 內部類：材料需求
	public static class MaterialRequirement {
		private String material;
		private int qtyNeeded;
		private int qtyOnHand;
		private int shortage;

		public MaterialRequirement() {
		}

		public MaterialRequirement(String material, int qtyNeeded, int qtyOnHand) {
			this.material = material;
			this.qtyNeeded = qtyNeeded;
			this.qtyOnHand = qtyOnHand;
			this.shortage = Math.max(0, qtyNeeded - qtyOnHand);
		}

		public String getMaterial() {
			return material;
		}

		public void setMaterial(String material) {
			this.material = material;
		}

		public int getQtyNeeded() {
			return qtyNeeded;
		}

		public void setQtyNeeded(int qtyNeeded) {
			this.qtyNeeded = qtyNeeded;
		}

		public int getQtyOnHand() {
			return qtyOnHand;
		}

		public void setQtyOnHand(int qtyOnHand) {
			this.qtyOnHand = qtyOnHand;
		}

		public int getShortage() {
			return shortage;
		}

		public void setShortage(int shortage) {
			this.shortage = shortage;
		}

		public boolean isShortage() {
			return shortage > 0;
		}

		// ✅ 新增：計算滿足率
		public double getSatisfactionRate() {
			return qtyNeeded > 0 ? (Math.min(qtyNeeded, qtyOnHand) * 100.0 / qtyNeeded) : 100.0;
		}
	}

	/**
	 * 計算工單在甘特圖中的開始位置百分比
	 */
	public double getGanttStartPosition(LocalDate timelineStart, LocalDate timelineEnd) {
		if (etaDate == null || timelineStart == null || timelineEnd == null)
			return 0.0;

		LocalDate workStartDate = etaDate.minusDays(getEstimatedDuration());
		long totalDays = ChronoUnit.DAYS.between(timelineStart, timelineEnd);
		long daysFromStart = ChronoUnit.DAYS.between(timelineStart, workStartDate);

		return totalDays > 0 ? Math.max(0, Math.min(100, (daysFromStart * 100.0) / totalDays)) : 0.0;
	}

	/**
	 * 計算工單在甘特圖中的寬度百分比
	 */
	public double getGanttWidthPosition(LocalDate timelineStart, LocalDate timelineEnd) {
		if (etaDate == null || timelineStart == null || timelineEnd == null)
			return 0.0;

		long totalDays = ChronoUnit.DAYS.between(timelineStart, timelineEnd);
		long workDuration = getEstimatedDuration();

		return totalDays > 0 ? Math.min(100, (workDuration * 100.0) / totalDays) : 0.0;
	}

	/**
	 * 估算工單所需工作天數
	 */
	public int getEstimatedDuration() {
		if (materials == null || materials.isEmpty())
			return 5; // 預設5天

		int totalUnits = materials.stream().mapToInt(MaterialRequirement::getQtyNeeded).sum();
		// 假設每天可處理24單位，向上取整
		return Math.max(1, (int) Math.ceil(totalUnits / 24.0));
	}

	/**
	 * 獲取工單的甘特圖顏色
	 */
	public String getGanttColor() {
		if (hasMaterialRisk()) {
			return "bg-red-500"; // 紅色：有材料風險
		} else if ("LATE".equals(status)) {
			return "bg-orange-500"; // 橙色：延遲風險
		} else if (isDueSoon()) {
			return "bg-yellow-500"; // 黃色：即將到期
		} else {
			return "bg-blue-500"; // 藍色：正常
		}
	}

	/**
	 * 獲取甘特圖文字顏色
	 */
	public String getGanttTextColor() {
		return "text-white";
	}

	/**
	 * 獲取工單在時間軸上的顯示文字
	 */
	public String getGanttDisplayText() {
		if (materials == null || materials.isEmpty()) {
			return machineName;
		}

		// 顯示主要材料類型
		String mainMaterial = materials.stream().max((m1, m2) -> Integer.compare(m1.getQtyNeeded(), m2.getQtyNeeded()))
				.map(MaterialRequirement::getMaterial).orElse(machineName);

		return machineName + "-" + mainMaterial;
	}

	/**
	 * 檢查工單是否在指定日期執行
	 */
	public boolean isExecutingOnDate(LocalDate date) {
		if (etaDate == null || date == null)
			return false;

		LocalDate startDate = etaDate.minusDays(getEstimatedDuration());
		return !date.isBefore(startDate) && !date.isAfter(etaDate);
	}

	/**
	 * 獲取工單的執行階段
	 */
	public String getExecutionPhase(LocalDate currentDate) {
		if (etaDate == null || currentDate == null)
			return "未安排";

		LocalDate startDate = etaDate.minusDays(getEstimatedDuration());

		if (currentDate.isBefore(startDate)) {
			return "等待中";
		} else if (currentDate.isAfter(etaDate)) {
			return "已完成";
		} else {
			long totalDays = getEstimatedDuration();
			long elapsedDays = ChronoUnit.DAYS.between(startDate, currentDate) + 1;
			int progress = (int) ((elapsedDays * 100) / totalDays);
			return "進行中 (" + progress + "%)";
		}
	}
}