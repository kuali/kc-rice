package org.kuali.rice.krms.framework.engine.expression;

import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kuali.rice.core.api.datetime.DateTimeService;
import org.kuali.rice.krms.api.engine.IncompatibleTypeException;

public class DateComparisonOperator implements EngineComparatorExtension, StringCoercionExtension {

	private Log LOG = LogFactory.getLog(DateComparisonOperator.class);
	
	private DateTimeService dateTimeService;

	public boolean canCoerce(String type, String value) {
		return coerce(type, value) != null;
	}

	@Override
	public Object coerce(String type, String value) {
		try {
			if (StringUtils.equals(type, java.util.Date.class.getCanonicalName())) {
				return dateTimeService.convertToDate(value);
			} else if (StringUtils.equals(type, java.sql.Date.class.getCanonicalName())) {
				return dateTimeService.convertToSqlDate(value);
			} else if (StringUtils.equals(type, java.sql.Timestamp.class.getCanonicalName())) {
				return dateTimeService.convertToSqlTimestamp(value);
			} else if (StringUtils.equals(type, java.sql.Time.class.getCanonicalName())) {
				return dateTimeService.convertToSqlTime(value);
			} else {
				return null;
			}
		} catch (ParseException e) {
			LOG.info("Unable to parse '" + value + "' into know date/time", e);
			return null;
		}
	}

	public DateTimeService getDateTimeService() {
		return dateTimeService;
	}

	public void setDateTimeService(DateTimeService dateTimeService) {
		this.dateTimeService = dateTimeService;
	}

	@Override
	public int compare(Object lhs, Object rhs) {
		if (lhs == null && rhs == null) {
			return 0;
		} else if (lhs == null) {
			return -1;
		} else if (rhs == null) {
			return 1;
		}
		
		Long lhsTime = ((Date) lhs).getTime();
		Long rhsTime;
		if (rhs instanceof Date) {
			rhsTime = ((Date) rhs).getTime();
		} else if (rhs instanceof String) {
			rhsTime = ((Date)coerce(lhs.getClass().getCanonicalName(), (String)rhs)).getTime();
		} else {
			throw new IncompatibleTypeException("Expected Date or String rhs and therefore unable to compare lhs(" + 
				lhs.getClass().getCanonicalName() + ") and rhs(" + rhs.getClass().getCanonicalName() + ")");
		}
		
		return lhsTime.compareTo(rhsTime);
	}

	@Override
	public boolean canCompare(Object lhs, Object rhs) {
		return isDateType(lhs) 
				&& (isDateType(rhs) 
				|| (rhs instanceof String && canCoerce(lhs.getClass().getCanonicalName(), (String)rhs)));
	}

	protected boolean isDateType(Object lhs) {
		return lhs instanceof java.util.Date || lhs instanceof java.sql.Date 
				|| lhs instanceof java.sql.Timestamp || lhs instanceof java.sql.Time;
	}

}
