/**
 * Reports are Freeware Code Snippets
 *
 * This report is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

import genj.gedcom.Fam;
import genj.gedcom.Gedcom;
import genj.gedcom.Indi;
import genj.gedcom.PointInTime;
import genj.gedcom.PropertyDate;
import genj.report.Report;
import genj.util.ReferenceSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * GenJ - Report
 * @author Nils Meier nils@meiers.net
 * @author Carsten M�ssig carsten.muessig@gmx.net
 */
public class ReportEvents extends Report {
    
    /** whether we sort by day-of-month or date */
    public boolean isSortDay = true;
    /** whether births should be reported */
    public boolean reportBirth = true;
    /** whether deaths should be reported */
    public boolean reportDeath = true;
    /** whether marriages should be reported */
    public boolean reportMarriage = true;
    /** day of the date limit */
    public int day = new GregorianCalendar().get(Calendar.DAY_OF_MONTH);
    /** month of the date limit */
    public int month = new GregorianCalendar().get(Calendar.MONTH)+1;
    /** year of the date limit */
    public int year = new GregorianCalendar().get(Calendar.YEAR);
    /** how the day should be handled */
    public int handleDay = 2;
    public String[] handleDays = { i18n("date.min"), i18n("date.max"), i18n("date.fix"), i18n("date.ignore")};
    /** how the day should be handled */
    public int handleMonth = 2;
    public String[] handleMonths = { i18n("date.min"), i18n("date.max"), i18n("date.fix"), i18n("date.ignore")};
    
    /** how the day should be handled */
    public int handleYear = 2;
    public String[] handleYears = { i18n("date.min"), i18n("date.max"), i18n("date.fix"), i18n("date.ignore")};
    
    /** this report's version */
    public static final String VERSION = "1.0";
    /** constants for indicating the sort order in the report output */
    private static final int DAY = 0;
    private static final int YEAR = 1;
    
    /**
     * Returns the version of this script
     */
    public String getVersion() {
        return VERSION;
    }
    
    /**
     * Returns the name of this report
     */
    public String getName() {
        return i18n("name");
    }
    
    /**
     * Some information about this report
     */
    public String getInfo() {
        return i18n("info");
    }
    
    public String getAuthor() {
        return "Nils Meier <nils@meiers.net>, Carsten M\u00FCssig <carsten.muessig@gmx.net>";
    }
    
    /**
     * @see genj.report.Report#accepts(java.lang.Object)
     */
    public String accepts(Object context) {
        // we accept only GEDCOM
        return context instanceof Gedcom ? getName() : null;
    }
    
    /**
     * Entry point into this report - by default reports are only run on a
     * context of type Gedcom. Depending on the logic in accepts either
     * an instance of Gedcom, Entity or Property can be passed in though.
     */
    public void start(Object context) {
        if((reportBirth)||(reportDeath)||(reportMarriage)) {
            // assuming Gedcom
            Gedcom gedcom = (Gedcom)context;
            
            ArrayList indis = new ArrayList(gedcom.getEntities(gedcom.INDI));
            ReferenceSet births = new ReferenceSet(), marriages = new ReferenceSet(), deaths = new ReferenceSet();
            
            for(int i=0;i<indis.size();i++) {
                
                Indi indi = (Indi)indis.get(i);
                
                if(reportBirth) {
                    if((indi.getBirthDate()!=null)&&(indi.getBirthDate().getStart()!=null)&&(checkDate(indi.getBirthDate().getStart())))
                        addToReferenceSet(indi.getBirthDate().getStart(), indi, births);
                }
                if(reportMarriage) {
                    Fam[] fams = indi.getFamilies();
                    for(int j=0;j<fams.length;j++) {
                        if((fams[j].getMarriageDate()!=null)&&(fams[j].getMarriageDate().getStart()!=null)&&(checkDate(fams[j].getMarriageDate().getStart())))
                            addToReferenceSet(fams[j].getMarriageDate().getStart(), indi, marriages);
                    }
                }
                if(reportDeath) {
                    if((indi.getDeathDate()!=null)&&(indi.getDeathDate().getStart()!=null)&&(checkDate(indi.getDeathDate().getStart())))
                        addToReferenceSet(indi.getDeathDate().getStart(), indi, deaths);
                }
            }
            
            println(i18n("day")+": "+day+" ("+getHandle(handleDay)+")");
            println(i18n("month")+": "+month+" ("+getHandle(handleMonth)+")");
            println(i18n("year")+": "+year+" ("+getHandle(handleYear)+")");
            println();
            
            if(reportBirth) {
                println("   "+i18n("birth"));
                report(births);
                println();
            }
            if(reportMarriage) {
                println("   "+i18n("marriage"));
                report(marriages);
                println();
            }
            if(reportDeath) {
                println("   "+i18n("death"));
                report(deaths);
            }
        }
    }
    
    private String getHandle(int what) {
        switch(what) {
            case 0: return i18n("date.min");
            case 1: return i18n("date.max");
            case 2: return i18n("date.fix");
            case 3: return i18n("date.ignore");
            default: return "";
        }
    }
    
    private void addToReferenceSet(PointInTime date, Indi indi, ReferenceSet set) {
        if(isSortDay) {
            int month = date.getMonth();
            if(set.getReferences(new Integer(month)).size()==0)
                set.add(new Integer(month), new ReferenceSet());
            ReferenceSet r = (ReferenceSet)set.getReferences(new Integer(month)).iterator().next();
            r.add(date, indi);
        }
        else
            set.add(date, indi);
    }
    
    private void report(ReferenceSet indis) {
        Iterator i = indis.getKeys(true).iterator();
        while(i.hasNext()) {
            Object key = i.next();
            if(key instanceof PointInTime)
                output(indis, (PointInTime)key);
            if(key instanceof Integer) {
                Iterator j = indis.getReferences(key).iterator();
                while(j.hasNext()) {
                    ReferenceSet r = (ReferenceSet)j.next();
                    Iterator k = r.getKeys(true).iterator();
                    ArrayList keys = new ArrayList();
                    while(k.hasNext())
                        keys.add((PointInTime)k.next());
                    Collections.sort(keys, new Comparator() {
                        public int compare(Object o1, Object o2) {
                            return ((PointInTime)o1).getDay()-((PointInTime)o2).getDay();
                        }
                    });
                    for(int l=0;l<keys.size();l++)
                        output(r, (PointInTime)keys.get(l));
                }
            }
        }
    }
    
    private void output(ReferenceSet indis, PointInTime date) {
        if(date.getDay()!=-1) {
            ArrayList list = new ArrayList(indis.getReferences(date));
            for(int j=0;j<list.size();j++) {
                Indi indi = (Indi)list.get(j);
                println("      "+date+" @"+indi.getId()+"@ "+indi.getName());
            }
        }
    }
    
    private boolean checkDate(PointInTime date) {
        boolean d = false, m = false, y = false;
        
        if((handleDay==0)&&(day<=(date.getDay()+1))) // day = minimum
            d = true;
        else if((handleDay==1)&&(day>=(date.getDay()+1))) // day = maximum
            d = true;
        else if((handleDay==2)&&(day==(date.getDay()+1))) // day = fix
            d = true;
        else if(handleDay==3) // day = ignore
            d = true;
        
        if((handleMonth==0)&&(month<=(date.getMonth()+1))) // month = minimum
            m = true;
        else if((handleMonth==1)&&(month>=(date.getMonth()+1))) // month = maximum
            m = true;
        else if((handleMonth==2)&&(month==(date.getMonth()+1))) // month = fix
            m = true;
        else if(handleMonth==3) // month = ignore
            m = true;
        
        if((handleYear==0)&&(year<=date.getYear())) // year = minimum
            y = true;
        else if((handleYear==1)&&(year>=date.getYear())) // year = maximum
            y = true;
        else if((handleYear==2)&&(year==date.getYear())) // year = fix
            y = true;
        else if(handleYear==3) // year = ignore
            y = true;
        
        if((d)&&(m)&&(y))
            return true;
        return false;
    }
}