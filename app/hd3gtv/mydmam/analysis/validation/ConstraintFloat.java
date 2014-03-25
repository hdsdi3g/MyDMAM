package hd3gtv.mydmam.analysis.validation;

class ConstraintFloat extends Constraint {
	
	private float reference;
	
	public ConstraintFloat(String rule, Comparator comparator, float reference) {
		super(rule, comparator);
		this.reference = reference;
	}
	
	protected boolean isInternalPassing(Object value) {
		float float_value;
		try {
			float_value = (Float) value;
		} catch (Exception e1) {
			if (value instanceof String) {
				try {
					float_value = Float.parseFloat((String) value);
				} catch (NumberFormatException e2) {
					return false;
				}
			} else if (value instanceof Number) {
				float_value = ((Number) value).floatValue();
			} else {
				return false;
			}
		}
		
		if (comparator == Comparator.EQUALS) {
			return reference == float_value;
		} else if (comparator == Comparator.DIFFERENT) {
			return reference != float_value;
		} else if (comparator == Comparator.EQUALS_OR_GREATER_THAN) {
			return reference <= float_value;
		} else if (comparator == Comparator.GREATER_THAN) {
			return reference < float_value;
		} else if (comparator == Comparator.EQUALS_OR_SMALLER_THAN) {
			return reference >= float_value;
		} else if (comparator == Comparator.SMALLER_THAN) {
			return reference > float_value;
		} else {
			return false;
		}
	}
	
	String getReference() {
		return String.valueOf(reference);
	}
	
}
