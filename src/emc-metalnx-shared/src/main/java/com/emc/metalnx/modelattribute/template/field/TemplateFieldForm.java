 /* Copyright (c) 2018, University of North Carolina at Chapel Hill */
 /* Copyright (c) 2015-2017, Dell EMC */
 


package com.emc.metalnx.modelattribute.template.field;

public class TemplateFieldForm {

	private Long id;
	private String templateName;
	private String attribute;
	private String attributeValue;
	private String attributeUnit;
	private float startRange;
	private float endRange;
	private int order;
	private Integer formListPosition;

	/**
	 * @return the attribute
	 */
	public String getAttribute() {
		return attribute;
	}

	/**
	 * @param attribute
	 *            the attribute to set
	 */
	public void setAttribute(final String attribute) {
		this.attribute = attribute;
	}

	/**
	 * @return the attributeValue
	 */
	public String getValue() {
		return attributeValue;
	}

	/**
	 * @param attributeValue
	 *            the attributeValue to set
	 */
	public void setValue(final String attributeValue) {
		this.attributeValue = attributeValue;
	}

	/**
	 * @return the attributeUnit
	 */
	public String getUnit() {
		return attributeUnit;
	}

	/**
	 * @param attributeUnit
	 *            the attributeUnit to set
	 */
	public void setUnit(final String attributeUnit) {
		this.attributeUnit = attributeUnit;
	}

	/**
	 * @return the startRange
	 */
	public float getStartRange() {
		return startRange;
	}

	/**
	 * @param startRange
	 *            the startRange to set
	 */
	public void setStartRange(final float startRange) {
		this.startRange = startRange;
	}

	/**
	 * @return the endRange
	 */
	public float getEndRange() {
		return endRange;
	}

	/**
	 * @param endRange
	 *            the endRange to set
	 */
	public void setEndRange(final float endRange) {
		this.endRange = endRange;
	}

	/**
	 * @return the order
	 */
	public int getOrder() {
		return order;
	}

	/**
	 * @param order
	 *            the order to set
	 */
	public void setOrder(final int order) {
		this.order = order;
	}

	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(final String templateName) {
		this.templateName = templateName;
	}

	/**
	 * @return the formListPosition
	 */
	public Integer getFormListPosition() {
		return formListPosition;
	}

	/**
	 * @param formListPosition
	 *            the formListPosition to set
	 */
	public void setFormListPosition(final Integer formListPosition) {
		this.formListPosition = formListPosition;
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(final Long id) {
		this.id = id;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof TemplateFieldForm) {
			TemplateFieldForm templateFieldForm = (TemplateFieldForm) obj;
			// checking if the ID is set
			if (getId() != null && templateFieldForm.getId() != null) {
				return getId().equals(templateFieldForm.getId());
			} else if (getAttribute() != null && getValue() != null && getUnit() != null) {
				return getAttribute().equals(templateFieldForm.getAttribute())
						&& getValue().equals(templateFieldForm.getValue())
						&& getUnit().equals(templateFieldForm.getUnit());
			}
		}

		return false;
	}

}
