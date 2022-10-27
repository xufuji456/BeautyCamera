#include <player/PlayerParam.h>
#include "MediaClock.h"

MediaClock::MediaClock() {
    m_pts        = 0;
    m_pause      = 0;
    m_speed      = 1.0;
    m_pts_drift  = 0;
    m_lastUpdate = 0;
    init();
}

MediaClock::~MediaClock() = default;

void MediaClock::init() {
    setClock(NAN);
}

void MediaClock::setClock(double pts) {
    double time = (double) av_gettime_relative() / 1000000.0;
    setClock(pts, time);
}

void MediaClock::setClock(double pts, double time) {
    this->m_pts = pts;
    this->m_lastUpdate = time;
    this->m_pts_drift = this->m_pts - time;
}

double MediaClock::getClock() const {
    if (m_pause) {
        return m_pts;
    } else {
        double time = (double) av_gettime_relative() / 1000000.0;
        return m_pts_drift + time - (time - m_lastUpdate) * (1.0 - m_speed);
    }
}

void MediaClock::setSpeed(double speed) {
    setClock(getClock());
    this->m_speed = speed;
}

double MediaClock::getSpeed() const {
    return m_speed;
}

void MediaClock::syncToSlave(MediaClock *slave) {
    double clock = getClock();
    double slave_clock = slave->getClock();
    if (!isnan(slave_clock) && (isnan(clock) || fabs(clock - slave_clock) > AV_NOSYNC_THRESHOLD)) {
        setClock(slave_clock);
    }
}

