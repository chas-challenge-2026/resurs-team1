package se.comerit.resurs.service;

//Private class containing the mutable scoring state as it travels through the scoring engine.
public class ScoringState {


    private int flagCount = 0;
    private boolean hardReject = false;

    private StringBuilder decisionReason = new StringBuilder();
    private StringBuilder scoringLog = new StringBuilder();
    private int kreditPoang;

    public ScoringState(int initialScore) {
        this.kreditPoang = initialScore;
    }


    public int getFlagCount() {
        return flagCount;
    }

    public void setFlagCount(int flagCount) {
        this.flagCount = flagCount;
    }

    public void incrementFlags(int flags){
        this.flagCount = flagCount + flags;
    }

    public boolean isHardReject() {
        return hardReject;
    }

    public void setHardReject(boolean hardReject) {
        this.hardReject = hardReject;
    }

    public StringBuilder getDecisionReason() {
        return decisionReason;
    }

    public void appendDecisionReason(String string) {
        decisionReason.append(string);
    }

    public StringBuilder getScoringLog() {
        return scoringLog;
    }

    public void appendScoringLog(String string) {
        scoringLog.append(string);
    }

    public int getPoints() {
        return kreditPoang;
    }

    public void removePoints(int kreditPoang) {
        this.kreditPoang = kreditPoang;
    }
    public void addPoints(int kreditPoang) {
        this.kreditPoang = kreditPoang;
    }









}
